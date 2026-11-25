package com.runetick;

import com.google.inject.Binder;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.VarPlayer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

@Slf4j
@PluginDescriptor(
	name = "RuneTick",
	internalName = "runetick",
	legacyDataDirectory = "runetick",
	description = "Grand Exchange prices and offers, synced with Runetick when you turn that on",
	tags = {"grand exchange", "ge", "prices", "flipping", "runetick", "overlay"}
)
public class RuneTickPlugin extends Plugin
{
	private static final long LINK_TIMEOUT_MINUTES = 5;
	private static final long FLIP_REFRESH_SECONDS = 20;
	private static final long PLAN_REFRESH_SECONDS = 60;
	private static final long LOCAL_SYNC_SECONDS = 45;
	/** Min gap between live sync POSTs — GCS user JSON rate-limits ~1 write/sec bursts. */
	private static final long SYNC_MIN_GAP_MS = 10_000L;
	private static final long SYNC_BACKOFF_MS = 60_000L;
	private static final int HISTORY_SYNC_LIMIT = 50;
	/** Reprice net worth every N game ticks (~0.6s each). */
	private static final int NET_WORTH_REVALUE_TICKS = 10;
	private static final long OFFER_GUARD_SECONDS = 60;
	private int netWorthTickCounter;
	/** Last real item name. A lookup that comes back as the id must not replace it. */
	private final Map<Integer, String> itemNames = new ConcurrentHashMap<>();
	/** One Runetick menu line per right-click. */
	private boolean runetickMenuAdded;
	private final Map<Integer, List<String>> watched = new ConcurrentHashMap<>();
	private final AtomicReference<Long> nextSyncAllowedAt = new AtomicReference<>(0L);
	private final AtomicBoolean syncCoalesce = new AtomicBoolean(false);
	private ScheduledFuture<?> syncDebounceTask;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private WornHoldings wornHoldings;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private RuneTickConfig config;

	@Inject
	private FirebaseSession session;

	@Inject
	private RuneTickApi api;

	@Inject
	private OfferSync sync;

	@Inject
	private RuneTickState state;

	@Inject
	private RuneTickOverlay overlay;

	@Inject
	private GeOfferOverlay geOfferOverlay;

	@Inject
	private ItemTipOverlay itemTipOverlay;

	@Inject
	private GeSlotOverlay geSlotOverlay;

	@Inject
	private PluginFiles pluginFiles;

	@Inject
	private CashTracker cashTracker;

	@Inject
	private BankHoldings bankHoldings;

	@Inject
	private InventoryHoldings inventoryHoldings;

	@Inject
	private NetWorthTracker netWorthTracker;

	@Inject
	private GeHistory geHistory;

	@Inject
	private Notifier notifier;

	@Inject
	private OfferGuard offerGuard;

	private final OfferTracker tracker = new OfferTracker();
	private final AtomicInteger watchedGeItem = new AtomicInteger(0);
	private final AtomicReference<String> lastSyncFingerprint = new AtomicReference<>("");

	private RuneTickPanel panel;
	private NavigationButton navButton;
	private LinkServer linkServer;
	private ScheduledFuture<?> refreshTask;
	private ScheduledFuture<?> planTask;
	private ScheduledFuture<?> syncTask;
	private ScheduledFuture<?> offerGuardTask;

	@Override
	public void configure(Binder binder)
	{
		binder.bind(OfferSync.LoggedOffers.class).to(ConfigLoggedOffers.class);
	}

	@Provides
	RuneTickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneTickConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		cashTracker.setCountBank(config.countBank());
		pluginFiles.use(getPluginDirectory());
		tracker.reset();

		panel = new RuneTickPanel(
			this::connect, this::disconnect, this::openSite, this::openItemOnSite, this::searchItems, this::inspectItem, itemManager);
		panel.onBankView(this::cycleBankView);
		panel.setBankView(config.bankView().toString());
		navButton = NavigationButton.builder()
			.tooltip("Runetick")
			.icon(RuneTickPanel.icon())
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		SwingUtilities.invokeLater(() ->
		{
			try
			{
				clientToolbar.openPanel(navButton);
			}
			catch (Throwable t)
			{
				log.error("Could not open the Runetick sidebar", t);
			}
		});
		overlayManager.add(overlay);
		overlayManager.add(geOfferOverlay);
		overlayManager.add(itemTipOverlay);
		overlayManager.add(geSlotOverlay);

		executor.submit(() ->
		{
			try
			{
				setLinked(session.isLinked());
			}
			catch (IOException | RuntimeException e)
			{
				log.error("Could not read Runetick sign-in", e);
			}
		});
		refreshTask = executor.scheduleWithFixedDelay(
			this::refreshFlips, FLIP_REFRESH_SECONDS, FLIP_REFRESH_SECONDS, TimeUnit.SECONDS);
		planTask = executor.scheduleWithFixedDelay(
			this::refreshPlanAndSignals, 5, PLAN_REFRESH_SECONDS, TimeUnit.SECONDS);
		syncTask = executor.scheduleWithFixedDelay(
			this::queueLocalSync, 15, LOCAL_SYNC_SECONDS, TimeUnit.SECONDS);
		offerGuardTask = executor.scheduleWithFixedDelay(
			this::queueOfferGuard, 8, OFFER_GUARD_SECONDS, TimeUnit.SECONDS);
		log.debug("RuneTick started");
	}

	@Override
	protected void shutDown()
	{
		tracker.reset();
		cashTracker.persist();
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
		overlayManager.remove(geOfferOverlay);
		overlayManager.remove(itemTipOverlay);
		overlayManager.remove(geSlotOverlay);
		if (refreshTask != null)
		{
			refreshTask.cancel(false);
			refreshTask = null;
		}
		if (planTask != null)
		{
			planTask.cancel(false);
			planTask = null;
		}
		if (syncTask != null)
		{
			syncTask.cancel(false);
			syncTask = null;
		}
		if (offerGuardTask != null)
		{
			offerGuardTask.cancel(false);
			offerGuardTask = null;
		}
		if (syncDebounceTask != null)
		{
			syncDebounceTask.cancel(false);
			syncDebounceTask = null;
		}
		offerGuard.clear();
		stopLinkServer();
		log.debug("RuneTick stopped");
	}

	private void publishLocalSnapshot()
	{
		state.setCash(cashTracker.getInventoryGp(), cashTracker.getBankGp(), cashTracker.isBankSeen());
		if (config.showGeHistory())
		{
			state.setRecentTrades(JournalCutover.after(geHistory.recent(200), state.getHistoryFrom(), 8));
		}
		else
		{
			state.setRecentTrades(java.util.Collections.emptyList());
		}
		refreshPanelSnapshot();
	}

	private void revalueNetWorthAndPublish()
	{
		if (config.showNetWorth())
		{
			netWorthTracker.revalue();
		}
		publishLocalSnapshot();
	}

	private void refreshPanelSnapshot()
	{
		if (panel == null)
		{
			return;
		}
		long minDream = Math.max(0, config.minDreamProfitGp());
		long minMargin = Math.max(0, config.minMarginPerUnit());
		List<RuneTickApi.FatSpread> soon = new ArrayList<>();
		for (RuneTickApi.FatSpread f : state.getFat())
		{
			if (SureSell.soonQty(f) > 0)
			{
				soon.add(f);
			}
		}
		soon.sort((a, b) -> Long.compare(SureSell.soonProfit(b), SureSell.soonProfit(a)));
		List<BoardLine> fatLines = new ArrayList<>();
		for (RuneTickApi.FatSpread f : soon)
		{
			if (fatLines.size() >= 8)
			{
				break;
			}
			int qty = SureSell.soonQty(f);
			int mins = SureSell.minutes(f);
			fatLines.add(BoardLine.of(
				f.getId(),
				f.getName(),
				"sell " + PriceFormat.gp(f.getAsk()),
				"+" + PriceFormat.gp(f.getMarginPerUnit()) + " gp · " + PriceFormat.gp(qty) + " · ~" + mins + "m"));
		}
		List<BoardLine> dipLines = new ArrayList<>();
		if (config.showFatAndDips())
		{
			for (RuneTickApi.Dip d : state.getDips())
			{
				if (d == null)
				{
					continue;
				}
				if (config.hideAlchDips() && d.isAlchFloor())
				{
					continue;
				}
				long ask = FocusCopy.dipAsk(d.getPrice());
				long margin = ask - d.getPrice();
				if (margin < minMargin)
				{
					continue;
				}
				if (!BoardCuts.dip(d, config.hideCheap(), config.hideSlow()))
				{
					continue;
				}
				if (dipLines.size() >= 8)
				{
					break;
				}
				int bankQty = config.scanBankItems() ? bankHoldings.qty(d.getId()) : 0;
				String total = "margin " + PriceFormat.gp(margin) + "/ea " + FocusCopy.dipPct(d);
				if (bankQty > 0)
				{
					total = total + " | bank " + bankQty;
				}
				dipLines.add(BoardLine.of(
					d.getId(),
					d.getName(),
					"buy " + PriceFormat.gp(d.getPrice()) + " -> sell " + PriceFormat.gp(ask),
					total));
			}
		}
		List<BoardLine> flipLines = new ArrayList<>();
		for (RuneTickApi.PlacedFlip flip : state.getPlaced())
		{
			if (flip == null || "done".equals(flip.getStatus()))
			{
				continue;
			}
			if (flipLines.size() >= 8)
			{
				break;
			}
			int bankQty = config.scanBankItems() ? bankHoldings.qty(flip.getItemId()) : 0;
			String total = "fill " + flip.getBought() + "/" + flip.getUnits();
			if (config.offerEta())
			{
				int rem;
				if ("selling".equals(flip.getStatus()))
				{
					rem = Math.max(0, flip.getBought() - flip.getSold());
				}
				else
				{
					rem = Math.max(0, flip.getUnits() - flip.getBought());
				}
				boolean sellLeg = "selling".equals(flip.getStatus());
				long legPrice = sellLeg ? flip.getAsk() : flip.getBid();
				String eta = offerGuard.etaFor(flip.getItemId(), rem, !sellLeg, legPrice);
				if (!eta.isEmpty())
				{
					total = total + " · left " + eta;
				}
			}
			if (bankQty > 0)
			{
				total = total + " | bank " + bankQty;
			}
			flipLines.add(BoardLine.of(
				flip.getItemId(),
				flip.getItemName(),
				"buy " + PriceFormat.gp(flip.getBid()) + " -> sell " + PriceFormat.gp(flip.getAsk()),
				total));
		}
		List<BoardLine> historyLines = new ArrayList<>();
		if (config.showGeHistory())
		{
			List<GeTrade> trades = state.getRecentTrades();
			for (int i = trades.size() - 1; i >= 0 && historyLines.size() < 8; i--)
			{
				GeTrade t = trades.get(i);
				String name = itemName(t.getItemId());
				historyLines.add(BoardLine.of(
					t.getItemId(),
					name,
					(t.isBuy() ? "bought @ " : "sold @ ") + PriceFormat.gp(t.getPrice()),
					"value " + PriceFormat.gp(t.getValue())));
			}
		}
		List<BoardLine> bankLines = Collections.emptyList();
		if (config.scanBankItems() && bankHoldings.isSeen())
		{
			bankLines = BankHits.build(
				bankHoldings.snapshot(),
				state.getFat(),
				state.getDips(),
				state.getPlaced(),
				minDream,
				minMargin,
				config.hideAlchDips(),
				8);
		}
		NetWorth worth = config.showNetWorth() ? netWorthTracker.snapshot() : NetWorth.empty();
		List<BoardLine> liveLines = buildLiveLines();
		panel.showSnapshot(new RuneTickPanel.Snapshot(
			state.isLinked(),
			state.isLoggedIn(),
			state.isSyncing(),
			state.isLoadingBoard(),
			state.isLoadingFlips(),
			state.getSyncStatus(),
			state.getInventoryGp(),
			state.getBankGp(),
			state.isBankSeen(),
			bankHoldings.isSeen(),
			cashTracker.getSavedAt(),
			config.showNetWorth(),
			worth,
			liveLines,
			bankLines,
			fatLines,
			dipLines,
			flipLines,
			historyLines));
	}

	private List<BoardLine> buildLiveLines()
	{
		List<BoardLine> out = new ArrayList<>();
		for (OfferGuard.LiveRow row : offerGuard.getLiveRows())
		{
			if (row == null || row.itemId <= 0)
			{
				continue;
			}
			if (out.size() >= 8)
			{
				break;
			}
			boolean sell = "sell".equals(row.side);
			String mid = (sell ? "Sell" : "Buy")
				+ " " + row.filled + "/" + row.total
				+ " @ " + PriceFormat.gpCompact(row.price);
			String total = row.eta.isEmpty() ? "" : ("left " + row.eta);
			if (sell)
			{
				CostBasis.Open paid = CostBasis.openBuys(geHistory.all(), row.itemId);
				if (paid != null)
				{
					long vs = FocusCopy.eachProfit(paid.each, row.price);
					String paidBit = "paid " + PriceFormat.gpCompact(paid.each);
					if (vs != Long.MIN_VALUE)
					{
						paidBit = paidBit + " · " + (vs > 0 ? "+" : "") + PriceFormat.gpCompact(vs);
					}
					total = total.isEmpty() ? paidBit : paidBit + " · " + total;
				}
			}
			if (row.alert != OfferAlert.Kind.NONE)
			{
				total = total.isEmpty() ? "watch price" : total + " · watch";
			}
			out.add(BoardLine.of(row.itemId, row.name, mid, total));
		}
		return out;
	}

	/** Snapshot live GE on client thread, then quote/ETA/alerts off-thread. */
	private void queueOfferGuard()
	{
		if (!state.isLinked() || !state.isLoggedIn())
		{
			return;
		}
		if (!config.offerEta() && !(config.offerPriceAlerts() && config.desktopNotify()))
		{
			return;
		}
		try
		{
			clientThread.invoke(() ->
			{
				try
				{
					if (client.getGameState() != GameState.LOGGED_IN)
					{
						return;
					}
					final List<RuneTickApi.GeLiveOffer> snap = snapshotLiveOffers();
					executor.submit(() ->
					{
						try
						{
							offerGuard.poll(snap, this::notifyDesktop);
							refreshPanelSnapshot();
						}
						catch (Throwable t)
						{
							log.error("OfferGuard poll failed", t);
						}
					});
				}
				catch (Throwable t)
				{
					log.error("OfferGuard snapshot failed", t);
				}
			});
		}
		catch (Throwable t)
		{
			log.error("OfferGuard schedule failed", t);
		}
	}

	private final AtomicInteger searchGen = new AtomicInteger();
	private final AtomicInteger inspectGen = new AtomicInteger();

	private void searchItems(String raw)
	{
		String q = raw == null ? "" : raw.trim();
		int gen = searchGen.incrementAndGet();
		if (q.length() < 2)
		{
			SwingUtilities.invokeLater(() -> panel.showSearch(Collections.emptyList(), ""));
			return;
		}
		executor.submit(() ->
		{
			try
			{
				List<BoardLine> lines = new ArrayList<>();
				for (RuneTickApi.SearchHit hit : api.search(q))
				{
					if (hit == null || hit.getId() <= 0 || lines.size() >= 6)
					{
						continue;
					}
					String buy = hit.getBuy() > 0 ? PriceFormat.gp(hit.getBuy()) : "-";
					String sell = hit.getSell() > 0 ? PriceFormat.gp(hit.getSell()) : "-";
					long each = FocusCopy.eachProfit(hit.getBuy(), hit.getSell());
					String profit = each == Long.MIN_VALUE
						? ""
						: ((each > 0 ? "+" : "") + PriceFormat.gp(each) + " each");
					lines.add(BoardLine.of(hit.getId(), hit.getName(), "buy " + buy + "  sell " + sell, profit));
					rememberPrice(hit.getId(), hit.getBuy(), hit.getSell());
				}
				if (gen != searchGen.get())
				{
					return;
				}
				SwingUtilities.invokeLater(() -> panel.showSearch(lines, "No match"));
			}
			catch (Throwable t)
			{
				log.error("Item search failed", t);
				if (gen == searchGen.get())
				{
					SwingUtilities.invokeLater(() -> panel.showSearch(Collections.emptyList(), "Search unavailable"));
				}
			}
		});
	}

	private void rememberPrice(int itemId, long buy, long sell)
	{
		long each = FocusCopy.eachProfit(buy, sell);
		String line = "buy " + PriceFormat.gp(buy) + " · sell " + PriceFormat.gp(sell);
		if (each != Long.MIN_VALUE)
		{
			line += " · " + (each > 0 ? "+" : "") + PriceFormat.gp(each);
		}
		state.rememberPrice(itemId, line);
	}

	private void loadWatch()
	{
		if (!state.isLinked())
		{
			SwingUtilities.invokeLater(() -> panel.showWatch(Collections.emptyList(), "connect first"));
			return;
		}
		executor.submit(() ->
		{
			try
			{
				RuneTickApi.Me me = api.getMe();
				List<Integer> ids = new ArrayList<>();
				Map<Integer, List<String>> where = new HashMap<>();
				if (me.getLists() != null)
				{
					for (RuneTickApi.WatchList list : me.getLists())
					{
						if (list != null && list.getItemIds() != null)
						{
							for (Integer id : list.getItemIds())
							{
								if (id == null)
								{
									continue;
								}
								if (!where.containsKey(id))
								{
									ids.add(id);
								}
								where.computeIfAbsent(id, k -> new ArrayList<>()).add(list.getId());
							}
						}
					}
				}
				watched.clear();
				watched.putAll(where);
				List<BoardLine> lines = new ArrayList<>();
				for (Integer id : ids)
				{
					if (id == null || id <= 0 || lines.size() >= 12)
					{
						continue;
					}
					RuneTickApi.ItemQuote quote = api.getItem(id);
					if (quote == null)
					{
						continue;
					}
					rememberPrice(id, quote.getBuy(), quote.getSell());
					long each = FocusCopy.eachProfit(quote.getBuy(), quote.getSell());
					String profit = each == Long.MIN_VALUE ? "" : ((each > 0 ? "+" : "") + PriceFormat.gp(each));
					lines.add(BoardLine.of(id, quote.getName(), "buy " + PriceFormat.gp(quote.getBuy()), profit));
				}
				SwingUtilities.invokeLater(() -> panel.showWatch(lines, lines.isEmpty() ? "nothing saved" : ""));
			}
			catch (Throwable t)
			{
				log.error("Watchlist failed", t);
				SwingUtilities.invokeLater(() -> panel.showWatch(Collections.emptyList(), "unavailable"));
			}
		});
	}

	private void watchItem(int itemId)
	{
		if (!state.isLinked())
		{
			panel.showMessage("Connect Runetick to save a watch");
			return;
		}
		executor.submit(() ->
		{
			try
			{
				api.addToWatchlist(itemId);
				watched.computeIfAbsent(itemId, k -> new ArrayList<>()).add("default");
				loadWatch();
				SwingUtilities.invokeLater(() -> panel.showMessage("Added to your watchlist"));
			}
			catch (Throwable t)
			{
				log.error("Could not watch item " + itemId, t);
				SwingUtilities.invokeLater(() -> panel.showMessage("Could not save that watch"));
			}
		});
	}

	private void inspectItem(int itemId)
	{
		if (itemId <= 0 || panel == null)
		{
			return;
		}
		int gen = inspectGen.incrementAndGet();
		SwingUtilities.invokeLater(panel::showItemPending);
		executor.submit(() ->
		{
			try
			{
				RuneTickApi.ItemQuote quote = api.getItem(itemId);
				if (gen != inspectGen.get())
				{
					return;
				}
				CostBasis.Open paid = CostBasis.openBuys(geHistory.all(), itemId);
				Long paidEach = paid == null ? null : paid.each;
				if (quote != null)
				{
					rememberPrice(itemId, quote.getBuy(), quote.getSell());
				}
				List<Long> spark;
				try
				{
					spark = api.getSpark(itemId);
				}
				catch (IOException e)
				{
					spark = Collections.emptyList();
				}
				Integer bag = inventoryHoldings.snapshot().get(itemId);
				RuneTickPanel.ItemExtras extras = new RuneTickPanel.ItemExtras(
					spark, bag == null ? 0 : bag, bankHoldings.qty(itemId), bankHoldings.isSeen(), offersFor(itemId),
					state.isLinked() ? isWatched(itemId) : null, () -> toggleWatch(itemId));
				if (gen != inspectGen.get())
				{
					return;
				}
				SwingUtilities.invokeLater(() -> panel.showItem(quote, paidEach, extras));
			}
			catch (Throwable t)
			{
				log.error("Item detail failed", t);
				if (gen == inspectGen.get())
				{
					SwingUtilities.invokeLater(() -> panel.showItem(null));
				}
			}
		});
	}

	private List<String> offersFor(int itemId)
	{
		List<String> out = new ArrayList<>();
		try
		{
			GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
			if (offers == null)
			{
				return out;
			}
			for (GrandExchangeOffer o : offers)
			{
				if (o == null || o.getItemId() != itemId || o.getState() == GrandExchangeOfferState.EMPTY)
				{
					continue;
				}
				boolean buy = o.getState() == GrandExchangeOfferState.BUYING || o.getState() == GrandExchangeOfferState.BOUGHT
					|| o.getState() == GrandExchangeOfferState.CANCELLED_BUY;
				out.add((buy ? "Buy " : "Sell ") + PriceFormat.gp(o.getQuantitySold()) + "/" + PriceFormat.gp(o.getTotalQuantity())
					+ " @ " + PriceFormat.gp(o.getPrice()));
			}
		}
		catch (Throwable t)
		{
			return out;
		}
		return out;
	}

	private void unwatchItem(int itemId)
	{
		if (!state.isLinked())
		{
			return;
		}
		executor.submit(() ->
		{
			try
			{
				List<String> lists = watched.get(itemId);
				for (String list : lists == null || lists.isEmpty() ? Collections.singletonList("default") : new ArrayList<>(lists))
				{
					api.removeFromList(list, itemId);
				}
				watched.remove(itemId);
				loadWatch();
				SwingUtilities.invokeLater(() -> panel.showMessage("Removed from your watchlist"));
			}
			catch (Throwable t)
			{
				log.error("Could not unwatch item " + itemId, t);
				SwingUtilities.invokeLater(() -> panel.showMessage("Could not remove that watch"));
			}
		});
	}

	boolean isWatched(int itemId)
	{
		return watched.containsKey(itemId);
	}

	void toggleWatch(int itemId)
	{
		if (isWatched(itemId))
		{
			unwatchItem(itemId);
		}
		else
		{
			watchItem(itemId);
		}
	}

	private int canonical(int itemId)
	{
		try
		{
			int id = itemManager.canonicalize(itemId);
			return id > 0 ? id : itemId;
		}
		catch (Throwable t)
		{
			return itemId;
		}
	}

	private static void addSub(Menu menu, String option, Runnable action)
	{
		menu.createMenuEntry(-1)
			.setOption(option)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> action.run());
	}

	private void showDetails(int itemId)
	{
		clientToolbar.openPanel(navButton);
		inspectItem(itemId);
	}

	private void chat(String text)
	{
		String safe = text.replace("<", "").replace(">", "");
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("<col=22c55e>Runetick</col> " + safe)
			.build());
	}

	private RuneTickApi.ItemQuote freshQuote(int itemId) throws IOException
	{
		RuneTickApi.ItemQuote quote = api.getItem(itemId);
		if (quote != null)
		{
			rememberPrice(itemId, quote.getBuy(), quote.getSell());
		}
		return quote;
	}

	private void priceCheck(int itemId)
	{
		String name = itemName(itemId);
		executor.submit(() ->
		{
			try
			{
				RuneTickApi.ItemQuote quote = freshQuote(itemId);
				chat(MenuCopy.priceCheck(quote != null && quote.getName() != null ? quote.getName() : name, quote));
			}
			catch (Throwable t)
			{
				log.error("Price check failed", t);
				chat("Could not reach Runetick for a price.");
			}
		});
	}

	private long heldQty(int itemId)
	{
		Integer bag = inventoryHoldings.snapshot().get(itemId);
		return (bag == null ? 0 : bag) + (long) bankHoldings.qty(itemId);
	}

	private void sellValue(int itemId)
	{
		String name = itemName(itemId);
		long qty = heldQty(itemId);
		boolean bankSeen = bankHoldings.isSeen();
		executor.submit(() ->
		{
			try
			{
				RuneTickApi.ItemQuote quote = freshQuote(itemId);
				String line = MenuCopy.sellValue(quote != null && quote.getName() != null ? quote.getName() : name, qty, quote);
				chat(bankSeen ? line : line + " Open your bank once to count it too.");
			}
			catch (Throwable t)
			{
				log.error("Sell value failed", t);
				chat("Could not reach Runetick for a price.");
			}
		});
	}

	private void slotCall(int slot)
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		if (offers == null || slot < 0 || slot >= offers.length || offers[slot] == null
			|| offers[slot].getState() == GrandExchangeOfferState.EMPTY)
		{
			return;
		}
		GrandExchangeOffer offer = offers[slot];
		GrandExchangeOfferState st = offer.getState();
		boolean buy = st == GrandExchangeOfferState.BUYING || st == GrandExchangeOfferState.BOUGHT
			|| st == GrandExchangeOfferState.CANCELLED_BUY;
		boolean done = st == GrandExchangeOfferState.BOUGHT || st == GrandExchangeOfferState.SOLD;
		int itemId = offer.getItemId();
		long price = offer.getPrice();
		int filled = offer.getQuantitySold();
		int total = offer.getTotalQuantity();
		String name = itemName(itemId);
		int pct = config.offerAlertPercent() < 1 ? 2 : config.offerAlertPercent();
		long floor = config.offerMarginFloor();
		executor.submit(() ->
		{
			try
			{
				if (done)
				{
					chat(name + ": " + (buy ? "bought" : "sold") + " " + PriceFormat.gp(filled) + ". Collect it.");
					return;
				}
				RuneTickApi.ItemQuote quote = offerGuard.quoteOf(itemId);
				if (quote == null)
				{
					quote = freshQuote(itemId);
				}
				OfferAlert.Kind kind = quote == null ? OfferAlert.Kind.NONE : OfferAlert.classify(
					buy, price, quote.getBuy(), quote.getSell(), quote.getAfterTaxSell(), 0, pct, floor);
				chat(MenuCopy.slotCall(name, buy, price, filled, total, quote, kind));
			}
			catch (Throwable t)
			{
				log.error("Slot call failed", t);
				chat("Could not reach Runetick for a price.");
			}
		});
	}

	private void openSite()
	{
		LinkBrowser.browse(siteBase());
	}

	private void openItemOnSite(int itemId)
	{
		if (itemId <= 0)
		{
			openSite();
			return;
		}
		clientThread.invoke(() ->
		{
			int id = itemId;
			try
			{
				id = itemManager.canonicalize(itemId);
			}
			catch (Throwable ignored)
			{
				// Keep raw id if canonicalize is unavailable off-thread / asserts.
			}
			final int finalId = id > 0 ? id : itemId;
			LinkBrowser.browse(SiteLinks.itemUrl(siteBase(), finalId));
		});
	}

	private String siteBase()
	{
		return config.siteUrl() == null || config.siteUrl().trim().isEmpty()
			? RuneTickConfig.DEFAULT_SITE_URL
			: config.siteUrl().trim();
	}

	/** Reflect the sign-in in the panel, overlay state, and flip cache. */
	private void setLinked(boolean linked)
	{
		state.setLinked(linked);
		panel.showState(linked, linked ? "Loading..." : "");
		refreshPanelSnapshot();
		if (linked)
		{
			state.setLoadingFlips(true);
			state.setLoadingBoard(true);
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				state.setSyncStatus("log in to sync");
				panel.showMessage("Log into OSRS to sync GE history");
			}
			refreshFlips();
			refreshPlanAndSignals();
			loadWatch();
			queueLocalSync();
		}
	}

	private void refreshFlips()
	{
		try
		{
			if (!session.isLinked())
			{
				state.setLinked(false);
				state.setLoadingFlips(false);
				return;
			}
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				state.setLoadingFlips(false);
				return;
			}
			state.setLoadingFlips(true);
			RuneTickApi.PlacedResult result = api.getPlacedResult();
			state.setLinked(true);
			state.setPlaced(result.getPlaced(), result.getSummary());
		}
		catch (FirebaseSession.NotLinkedException e)
		{
			state.setLinked(false);
			panel.showState(false, e.getMessage());
		}
		catch (IOException | RuntimeException e)
		{
			log.error("Could not refresh Runetick flips", e);
			state.setNote("Could not load flips");
		}
		finally
		{
			state.setLoadingFlips(false);
			refreshPanelSnapshot();
		}
	}

	private void refreshPlanAndSignals()
	{
		try
		{
			if (!session.isLinked() || client.getGameState() != GameState.LOGGED_IN)
			{
				state.setLoadingBoard(false);
				return;
			}

			state.setLoadingBoard(true);
			RuneTickApi.Me me = api.getMe();
			Accent.set(me.getAccent());
			int slots = me.getSlots() > 0 ? me.getSlots() : 8;
			// Fat dream stacks size from account capital (default 125m), not inv-only cash.
			// Live bag cash alone was shrinking dreams after sync; bank-seen wealth can raise them.
			long account = me.getCapitalGp() > 0 ? me.getCapitalGp() : 125_000_000L;
			long known = cashTracker.getTotalKnownGp();
			long capital = Math.max(account, 125_000_000L);
			if (cashTracker.isBankSeen() && known > capital)
			{
				capital = known;
			}
			String pace = me.getCheckinMinutes() > 0 && me.getCheckinMinutes() <= 10 ? "fast" : "steady";

			// Steady plan still returns fat[]; dips are the monthly-median board.
			RuneTickApi.Plan plan = api.getPlan(capital, slots, pace);
			state.setPlan(plan);

			if (config.showFatAndDips())
			{
				List<RuneTickApi.Dip> dips = api.getDips(24).getDips();
				if (config.hideAlchDips() && dips != null)
				{
					List<RuneTickApi.Dip> filtered = new ArrayList<>();
					for (RuneTickApi.Dip d : dips)
					{
						if (d != null && !d.isAlchFloor())
						{
							filtered.add(d);
						}
					}
					dips = filtered;
				}
				state.setDips(dips);
			}
			else
			{
				state.setDips(java.util.Collections.emptyList());
			}
		}
		catch (FirebaseSession.NotLinkedException e)
		{
			state.setLinked(false);
			panel.showState(false, e.getMessage());
		}
		catch (IOException | RuntimeException e)
		{
			log.error("Could not refresh Runetick plan/signals", e);
			state.setNote("Could not load board");
		}
		finally
		{
			state.setLoadingBoard(false);
			refreshPanelSnapshot();
		}
	}

	/**
	 * Resolve item names on the client thread, then POST cash + GE history + live offers + net worth.
	 * No-op until the player is linked and logged into a character (history is per-profile).
	 */
	/**
	 * Coalesce offer-driven syncs so login floods / tick fills do not 429 GCS writes.
	 */
	private void requestLocalSync()
	{
		syncCoalesce.set(true);
		long now = System.currentTimeMillis();
		long allowed = nextSyncAllowedAt.get() == null ? 0L : nextSyncAllowedAt.get();
		long delayMs = Math.max(250L, allowed - now);
		if (syncDebounceTask != null && !syncDebounceTask.isDone())
		{
			return;
		}
		syncDebounceTask = executor.schedule(this::queueLocalSync, delayMs, TimeUnit.MILLISECONDS);
	}

	private void queueLocalSync()
	{
		syncCoalesce.set(false);
		if (!config.syncHistory())
		{
			return;
		}
		try
		{
			if (!session.isLinked())
			{
				return;
			}
		}
		catch (IOException e)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			state.setSyncStatus("log in to sync");
			return;
		}
		long now = System.currentTimeMillis();
		long allowed = nextSyncAllowedAt.get() == null ? 0L : nextSyncAllowedAt.get();
		if (now < allowed)
		{
			requestLocalSync();
			return;
		}

		clientThread.invoke(() ->
		{
			try
			{
				if (config.showNetWorth())
				{
					netWorthTracker.revalue();
				}
				List<GeTrade> trades = geHistory.recent(HISTORY_SYNC_LIMIT);
				Map<Integer, String> names = new HashMap<>();
				for (GeTrade t : trades)
				{
					if (t == null || names.containsKey(t.getItemId()))
					{
						continue;
					}
					names.put(t.getItemId(), itemName(t.getItemId()));
				}
				List<RuneTickApi.GeLiveOffer> geOffers = snapshotLiveOffers();
				RuneTickApi.NetWorthSnap netWorth = snapshotNetWorth();
				long inv = cashTracker.getInventoryGp();
				long bank = cashTracker.getBankGp();
				boolean bankSeen = cashTracker.isBankSeen();
				String fp = fingerprint(trades, inv, bank, bankSeen, geOffers, netWorth);
				if (fp.equals(lastSyncFingerprint.get()) && !state.getSyncStatus().isEmpty()
					&& !state.getSyncStatus().startsWith("could not"))
				{
					return;
				}
				final List<GeTrade> snapshot = trades;
				final Map<Integer, String> nameMap = names;
				final List<RuneTickApi.GeLiveOffer> offerSnap = geOffers;
				final RuneTickApi.NetWorthSnap worthSnap = netWorth;
				executor.submit(() -> pushLocalSync(snapshot, nameMap, inv, bank, bankSeen, offerSnap, worthSnap, fp));
			}
			catch (Throwable t)
			{
				log.error("Could not prepare GE history sync", t);
			}
		});
	}

	/** Build live GE slot rows (client thread). Empty slots omitted. */
	private List<RuneTickApi.GeLiveOffer> snapshotLiveOffers()
	{
		List<RuneTickApi.GeLiveOffer> out = new ArrayList<>();
		GrandExchangeOffer[] offers;
		try
		{
			offers = client.getGrandExchangeOffers();
		}
		catch (Throwable t)
		{
			return out;
		}
		if (offers == null)
		{
			return out;
		}
		for (int i = 0; i < offers.length; i++)
		{
			GrandExchangeOffer offer = offers[i];
			if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY || offer.getItemId() <= 0)
			{
				continue;
			}
			RuneTickApi.GeLiveOffer row = new RuneTickApi.GeLiveOffer();
			row.setSlot(i);
			row.setItemId(offer.getItemId());
			row.setItemName(itemName(offer.getItemId()));
			GrandExchangeOfferState st = offer.getState();
			boolean buy = st == GrandExchangeOfferState.BUYING
				|| st == GrandExchangeOfferState.BOUGHT
				|| st == GrandExchangeOfferState.CANCELLED_BUY;
			row.setSide(buy ? "buy" : "sell");
			row.setState(st == null ? "" : st.name());
			row.setPrice(offer.getPrice());
			row.setTotal(offer.getTotalQuantity());
			row.setFilled(offer.getQuantitySold());
			row.setSpent(offer.getSpent());
			out.add(row);
		}
		return out;
	}

	private RuneTickApi.NetWorthSnap snapshotNetWorth()
	{
		NetWorth worth = netWorthTracker.snapshot();
		RuneTickApi.NetWorthSnap snap = new RuneTickApi.NetWorthSnap();
		snap.setCashGp(worth.cashGp);
		snap.setInventoryCashGp(worth.inventoryCashGp);
		snap.setInvItemsGp(worth.invItemsGp);
		snap.setWornGp(worth.wornGp);
		snap.setBankExcluded(!worth.bankCounted);
		snap.setBankItemsGp(worth.bankItemsGp);
		snap.setOffersGp(worth.offersGp);
		snap.setInvGeGp(worth.invGeGp);
		snap.setTotalGp(worth.totalGp);
		snap.setBankItemsSeen(worth.bankItemsSeen);
		snap.setSessionDeltaGp(worth.sessionDeltaGp);
		snap.setSessionArmed(worth.sessionArmed);
		snap.setAt(System.currentTimeMillis());
		return snap;
	}

	private void pushLocalSync(List<GeTrade> trades, Map<Integer, String> names,
		long inv, long bank, boolean bankSeen,
		List<RuneTickApi.GeLiveOffer> geOffers, RuneTickApi.NetWorthSnap netWorth,
		String fingerprint)
	{
		state.setSyncing(true);
		state.setSyncStatus("syncing...");
		panel.showMessage("Syncing GE + net worth...");
		refreshPanelSnapshot();
		try
		{
			boolean wealth = config.sendWealth() && config.showNetWorth();
			RuneTickApi.SyncResult result = api.syncLocal(inv, bank, bankSeen, trades, names, geOffers, wealth ? netWorth : null, wealth, config.countBank());
			lastSyncFingerprint.set(fingerprint);
			nextSyncAllowedAt.set(System.currentTimeMillis() + SYNC_MIN_GAP_MS);
			Long previousCutover = state.getHistoryFrom();
			Long nextCutover = result.getHistoryFrom();
			state.setHistoryFrom(nextCutover);
			if (JournalCutover.journalReset(previousCutover, nextCutover))
			{
				netWorthTracker.rebaseSession();
				publishLocalSnapshot();
			}
			int live = geOffers == null ? 0 : geOffers.size();
			String msg;
			if (result.getAdded() > 0)
			{
				msg = "Synced " + result.getAdded() + " trade" + (result.getAdded() == 1 ? "" : "s");
			}
			else if (live > 0)
			{
				msg = live + " live GE slot" + (live == 1 ? "" : "s") + " on site";
			}
			else if (trades.isEmpty())
			{
				msg = "No GE history yet";
			}
			else
			{
				msg = "History up to date";
			}
			state.setSyncStatus(msg);
			panel.showMessage(msg + " - on the website");
			if (result.getPlacedUpdated() > 0)
			{
				executor.submit(this::refreshFlips);
			}
			if (syncCoalesce.get())
			{
				requestLocalSync();
			}
		}
		catch (FirebaseSession.NotLinkedException e)
		{
			state.setLinked(false);
			state.setSyncStatus("");
			panel.showState(false, e.getMessage());
		}
		catch (IOException | RuntimeException e)
		{
			log.error("Could not sync GE history to Runetick", e);
			// HTTP 500 from GCS 429 comes through as sync_failed — back off hard either way.
			nextSyncAllowedAt.set(System.currentTimeMillis() + SYNC_BACKOFF_MS);
			state.setSyncStatus("could not sync");
			panel.showMessage("Could not sync history; see error.log");
		}
		finally
		{
			state.setSyncing(false);
			refreshPanelSnapshot();
		}
	}

	static String fingerprint(List<GeTrade> trades, long inv, long bank, boolean bankSeen,
		List<RuneTickApi.GeLiveOffer> geOffers, RuneTickApi.NetWorthSnap netWorth)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(inv).append(':').append(bank).append(':').append(bankSeen).append('|');
		if (netWorth != null)
		{
			sb.append(netWorth.getWornGp()).append(':').append(netWorth.isBankExcluded()).append('|');
		}
		if (trades != null)
		{
			for (GeTrade t : trades)
			{
				if (t == null)
				{
					continue;
				}
				sb.append(t.getItemId()).append(',')
					.append(t.isBuy() ? 'B' : 'S').append(',')
					.append(t.getQuantity()).append(',')
					.append(t.getPrice()).append(',')
					.append(t.getTime()).append(';');
			}
		}
		sb.append('|');
		if (geOffers != null)
		{
			for (RuneTickApi.GeLiveOffer o : geOffers)
			{
				if (o == null)
				{
					continue;
				}
				sb.append(o.getSlot()).append(',')
					.append(o.getItemId()).append(',')
					.append(o.getState()).append(',')
					.append(o.getFilled()).append(',')
					.append(o.getSpent()).append(';');
			}
		}
		sb.append('|');
		if (netWorth != null)
		{
			sb.append(netWorth.getTotalGp()).append(',')
				.append(netWorth.getInvGeGp()).append(',')
				.append(netWorth.getOffersGp()).append(',')
				.append(netWorth.isBankItemsSeen()).append(',')
				.append(netWorth.getSessionDeltaGp());
		}
		return sb.toString();
	}

	/**
	 * @param realId already-canonicalized tradeable id (canonicalize must run on the client thread)
	 */
	private void refreshGeItem(int realId)
	{
		if (realId <= 0)
		{
			state.clearGeItem();
			return;
		}
		try
		{
			RuneTickApi.ItemQuote quote = api.getItem(realId);
			if (quote == null)
			{
				state.clearGeItem();
				return;
			}
			state.setGeItem(realId, quote);
		}
		catch (Throwable t)
		{
			log.error("Could not load Runetick tip for item " + realId, t);
		}
	}

	/**
	 * Resolve notes → real item id on the client thread, then fetch off-thread.
	 * ItemManager.canonicalize asserts if called off the client thread (-ea), which
	 * previously took the whole client down when a GE item was selected.
	 */
	private void queueGeItemRefresh(int itemId)
	{
		if (itemId <= 0)
		{
			state.clearGeItem();
			return;
		}
		clientThread.invoke(() ->
		{
			try
			{
				int realId = itemManager.canonicalize(itemId);
				if (realId <= 0)
				{
					realId = itemId;
				}
				final int id = realId;
				executor.submit(() -> refreshGeItem(id));
			}
			catch (Throwable t)
			{
				log.error("Could not resolve GE item " + itemId, t);
			}
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		try
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}
			cashTracker.refreshInventory();
			if (config.showNetWorth())
			{
				netWorthTickCounter++;
				if (netWorthTickCounter >= NET_WORTH_REVALUE_TICKS)
				{
					netWorthTickCounter = 0;
					inventoryHoldings.refresh(client.getItemContainer(InventoryID.INVENTORY));
					ItemContainer bank = client.getItemContainer(InventoryID.BANK);
					if (bank != null)
					{
						cashTracker.refreshBank();
						bankHoldings.refresh(bank);
					}
					revalueNetWorthAndPublish();
				}
				else
				{
					publishLocalSnapshot();
				}
			}
			else
			{
				publishLocalSnapshot();
			}

			if (!config.showGeTips())
			{
				return;
			}
			int itemId = GeView.tipItemId(client);
			int previous = watchedGeItem.getAndSet(itemId);
			if (itemId != previous)
			{
				queueGeItemRefresh(itemId);
			}
		}
		catch (Throwable t)
		{
			log.error("Error on game tick", t);
		}
	}

	/** Client thread only: prices come from the item manager. */
	private void rebuildItemMarks()
	{
		java.util.Map<Integer, Long> prices = new java.util.HashMap<>();
		java.util.function.ToLongFunction<Integer> priceOf = itemId -> prices.computeIfAbsent(itemId, k ->
		{
			try
			{
				return (long) Math.max(0, itemManager.getItemPrice(k));
			}
			catch (Throwable t)
			{
				return 0L;
			}
		});
		List<GeTrade> trades = geHistory == null ? java.util.Collections.<GeTrade>emptyList() : geHistory.all();
		java.util.Set<Integer> bought = new java.util.HashSet<>();
		for (GeTrade trade : trades)
		{
			if (trade != null && trade.isBuy())
			{
				bought.add(trade.getItemId());
			}
		}
		java.util.Set<Integer> held = new java.util.HashSet<>(bankHoldings.snapshot().keySet());
		held.addAll(inventoryHoldings.snapshot().keySet());
		held.retainAll(bought);
		state.setItemMarks(
			BankView.valueMarks(bankHoldings.snapshot(), priceOf, config.bankValueMin()),
			BankView.profitMarks(held, priceOf, itemId ->
			{
				CostBasis.Open paid = CostBasis.openBuys(trades, itemId);
				return paid == null ? 0L : paid.each;
			}));
	}

	private void cycleBankView()
	{
		BankView.Mode next = config.bankView().next();
		configManager.setConfiguration(RuneTickConfig.GROUP, "bankView", next);
		clientThread.invokeLater(this::rebuildItemMarks);
		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.setBankView(next.toString()));
		}
	}

	private boolean depositBoxOpen()
	{
		try
		{
			Widget box = client.getWidget(InterfaceID.BankDepositbox.UNIVERSE);
			return box != null && !box.isHidden();
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		try
		{
			int id = event.getContainerId();
			if (id == InventoryID.EQUIPMENT.getId())
			{
				wornHoldings.refresh(client.getItemContainer(InventoryID.EQUIPMENT));
				revalueNetWorthAndPublish();
			}
			else if (id == InventoryID.INVENTORY.getId())
			{
				Map<Integer, Integer> beforeItems = inventoryHoldings.snapshot();
				long beforeCoins = cashTracker.getInventoryGp();
				boolean depositBox = depositBoxOpen();
				cashTracker.refreshInventory();
				inventoryHoldings.refresh(client.getItemContainer(InventoryID.INVENTORY));
				if (depositBox)
				{
					long afterCoins = cashTracker.getInventoryGp();
					if (beforeCoins > afterCoins)
					{
						cashTracker.applyCoinDeposit(beforeCoins - afterCoins);
					}
					bankHoldings.applyDeposit(
						DepositToBank.removed(beforeItems, inventoryHoldings.snapshot()),
						client.getItemContainer(InventoryID.BANK));
				}
				revalueNetWorthAndPublish();
			}
			else if (id == InventoryID.BANK.getId())
			{
				cashTracker.refreshBank();
				bankHoldings.refresh(client.getItemContainer(InventoryID.BANK));
				revalueNetWorthAndPublish();
			}
			if (id == InventoryID.INVENTORY.getId() || id == InventoryID.BANK.getId())
			{
				rebuildItemMarks();
			}
		}
		catch (Throwable t)
		{
			log.error("Error reading cash containers", t);
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		runetickMenuAdded = false;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		try
		{
			if (runetickMenuAdded || !config.openItemOnSite() || event == null)
			{
				return;
			}
			String option = event.getOption();
			if (option == null)
			{
				return;
			}
			int itemId = event.getItemId();
			if (itemId <= 0)
			{
				int slot = GeView.slotFromMenu(client, event.getActionParam1());
				if (slot >= 0)
				{
					itemId = GeView.itemInSlot(client, slot);
				}
				else if (GeView.offerScreenOpen(client) && "Back".equals(option))
				{
					itemId = GeView.tipItemId(client);
				}
			}
			if (itemId <= 0)
			{
				return;
			}
			// One entry per menu. A GE slot menu has both View offer and Abort offer.
			if (!"Examine".equals(option) && !"View offer".equals(option) && !"Back".equals(option))
			{
				return;
			}
			if (config.menuNeedsShift() && !client.isKeyPressed(KeyCode.KC_SHIFT))
			{
				return;
			}
			final int id = canonical(itemId);
			final int slot = "View offer".equals(option) ? GeView.slotFromMenu(client, event.getActionParam1()) : -1;
			runetickMenuAdded = true;
			MenuEntry top = client.createMenuEntry(-1)
				.setOption("<col=22c55e>Runetick</col>")
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> showDetails(id));
			Menu sub = top.createSubMenu();
			addSub(sub, "Details", () -> showDetails(id));
			addSub(sub, "Price check", () -> priceCheck(id));
			if (slot >= 0)
			{
				addSub(sub, "What now?", () -> slotCall(slot));
			}
			else
			{
				addSub(sub, "Sell value", () -> sellValue(id));
			}
			if (state.isLinked())
			{
				if (isWatched(id))
				{
					addSub(sub, "Unwatch", () -> unwatchItem(id));
				}
				else
				{
					addSub(sub, "Watch", () -> watchItem(id));
				}
			}
			addSub(sub, "Open on site", () -> openItemOnSite(id));
		}
		catch (Throwable t)
		{
			log.error("Error adding Runetick menu entry", t);
		}
	}

	/** Keep the last real name. A lookup that returns the id, or "null", must not replace it. */
	private String itemName(int itemId)
	{
		if (itemId <= 0)
		{
			return "Item";
		}
		String cached = itemNames.get(itemId);
		try
		{
			String name = itemManager.getItemComposition(itemId).getName();
			if (name != null)
			{
				name = name.trim();
			}
			if (name != null && !name.isEmpty() && !"null".equalsIgnoreCase(name) && !name.equals(Integer.toString(itemId)))
			{
				itemNames.put(itemId, name);
				return name;
			}
		}
		catch (Throwable t)
		{
			// Composition asserts off the client thread. The cached name still stands.
		}
		return cached != null ? cached : ("Item " + itemId);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		try
		{
			// GE offer create type changes (buy vs sell) — tip panel re-renders from state.
			if (event.getVarpId() == VarPlayer.CURRENT_GE_ITEM || event.getVarbitId() == net.runelite.api.gameval.VarbitID.GE_SELECTEDSLOT)
			{
				int itemId = GeView.tipItemId(client);
				watchedGeItem.set(itemId);
				queueGeItemRefresh(itemId);
			}
		}
		catch (Throwable t)
		{
			log.error("Error handling GE item change", t);
		}
	}

	/** Client thread: switch cash, bank and session to the character now logged in. */
	private void useAccount()
	{
		long hash;
		try
		{
			hash = client.getAccountHash();
		}
		catch (Throwable t)
		{
			return;
		}
		if (hash == -1)
		{
			return;
		}
		cashTracker.setCountBank(config.countBank());
		if (cashTracker.useAccount(hash))
		{
			bankHoldings.useAccount();
			netWorthTracker.onAccountChange();
			lastSyncFingerprint.set("");
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!RuneTickConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		String key = event.getKey();
		if ("countBank".equals(key) || "sendWealth".equals(key) || "showNetWorth".equals(key))
		{
			clientThread.invokeLater(() ->
			{
				cashTracker.setCountBank(config.countBank());
				if ("countBank".equals(key))
				{
					netWorthTracker.rebaseSession();
				}
				revalueNetWorthAndPublish();
				lastSyncFingerprint.set("");
				requestLocalSync();
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gs = event.getGameState();
		state.setLoggedIn(gs == GameState.LOGGED_IN);

		// Offers are re-sent after login; forget offer tracker so an account switch isn't a repeat.
		// Keep last-known cash (memory + disk) until live containers refresh it.
		if (gs == GameState.LOGIN_SCREEN || gs == GameState.LOGGING_IN)
		{
			tracker.reset();
			cashTracker.onLogout();
			netWorthTracker.onLogout();
			offerGuard.clear();
			watchedGeItem.set(0);
			state.clearGeItem();
			state.setCash(cashTracker.getInventoryGp(), cashTracker.getBankGp(), cashTracker.isBankSeen());
			state.setRecentTrades(java.util.Collections.emptyList());
			lastSyncFingerprint.set("");
			if (gs == GameState.LOGIN_SCREEN)
			{
				state.setSyncStatus(state.isLinked() ? "log in to sync" : "");
			}
			refreshPanelSnapshot();
		}
		else if (gs == GameState.LOGGED_IN)
		{
			state.setSyncStatus(state.isLinked() ? "syncing..." : "");
			useAccount();
			cashTracker.refreshInventory();
			inventoryHoldings.refresh(client.getItemContainer(InventoryID.INVENTORY));
			wornHoldings.refresh(client.getItemContainer(InventoryID.EQUIPMENT));
			state.setCash(cashTracker.getInventoryGp(), cashTracker.getBankGp(), cashTracker.isBankSeen());
			revalueNetWorthAndPublish();
			queueLocalSync();
			queueOfferGuard();
			executor.submit(this::refreshFlips);
			executor.submit(this::refreshPlanAndSignals);
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		try
		{
			GrandExchangeOffer offer = event.getOffer();
			if (offer == null)
			{
				return;
			}

			if (offer.getState() == GrandExchangeOfferState.EMPTY)
			{
				// The client blanks every slot while logging in or hopping; only a real in-game clear counts.
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					tracker.track(event.getSlot(), offer, System.currentTimeMillis());
					sync.slotEmptied(event.getSlot());
					revalueNetWorthAndPublish();
					lastSyncFingerprint.set("");
					requestLocalSync();
				}
				return;
			}

			OfferEvent offerEvent = tracker.track(event.getSlot(), offer, System.currentTimeMillis());
			revalueNetWorthAndPublish();
			// Mirror live slots + net worth (debounced — login floods used to 429 GCS).
			lastSyncFingerprint.set("");
			requestLocalSync();
			queueOfferGuard();
			if (offerEvent == null)
			{
				return;
			}

			log.debug("GE offer update: {}", offerEvent);
			// getItemComposition asserts on the wrong thread; never let that kill the client.
			String itemName;
			try
			{
				itemName = itemManager.getItemComposition(offerEvent.getItemId()).getName();
			}
			catch (Throwable t)
			{
				itemName = "item " + offerEvent.getItemId();
			}
			final String name = itemName;
			executor.submit(() -> syncOffer(offerEvent, name));
		}
		catch (Throwable t)
		{
			log.error("Error handling Grand Exchange offer update", t);
		}
	}

	private void syncOffer(OfferEvent offerEvent, String itemName)
	{
		try
		{
			OfferSync.Result result = sync.handle(offerEvent, itemName);
			if (result == OfferSync.Result.PLACED_UPDATED)
			{
				String msg = "Updated your " + itemName + " flip";
				panel.showMessage(msg);
				if (config.notifyFlipUpdates()) notifyDesktop(msg);
				refreshFlips();
			}
			else if (result == OfferSync.Result.TRADE_LOGGED)
			{
				String msg = "Logged " + (offerEvent.isBuy() ? "buy" : "sell") + ": " + itemName;
				panel.showMessage(msg);
				if (config.notifyLoggedTrades()) notifyDesktop(msg);
			}
		}
		catch (FirebaseSession.NotLinkedException e)
		{
			state.setLinked(false);
			panel.showState(false, e.getMessage());
		}
		catch (IOException | RuntimeException e)
		{
			log.error("Could not sync " + itemName + " offer to Runetick", e);
			panel.showMessage("Could not reach Runetick; see error.log");
		}
	}

	private void notifyDesktop(String message)
	{
		if (!config.desktopNotify() || message == null || message.isEmpty())
		{
			return;
		}
		try
		{
			notifier.notify(message);
		}
		catch (Throwable t)
		{
			log.error("Desktop notify failed", t);
		}
	}

	private void connect()
	{
		executor.submit(() ->
		{
			try
			{
				HttpUrl site = HttpUrl.parse(config.siteUrl() == null ? "" : config.siteUrl().trim());
				if (site == null)
				{
					panel.showMessage("The Runetick address in the plugin settings is not a valid URL");
					return;
				}

				stopLinkServer();
				LinkServer server = new LinkServer(this::linked);
				setLinkServer(server);
				executor.schedule(server::stop, LINK_TIMEOUT_MINUTES, TimeUnit.MINUTES);

				HttpUrl url = site.newBuilder()
					.addPathSegment("link")
					.addQueryParameter("port", String.valueOf(server.getPort()))
					.addQueryParameter("state", server.getState())
					.build();
				panel.showMessage("Finish connecting in your browser");
				LinkBrowser.browse(url.toString());
			}
			catch (IOException | RuntimeException e)
			{
				log.error("Could not start Runetick connect", e);
				panel.showMessage("Could not start connecting; see error.log");
			}
		});
	}

	/** Called by the link server when the browser hands over the sign-in. */
	private void linked(String apiKey, String refreshToken) throws IOException
	{
		try
		{
			session.link(apiKey, refreshToken);
			// Prove the sign-in works before telling the player they are connected.
			session.getIdToken();
			setLinked(true);
		}
		catch (IOException | RuntimeException e)
		{
			log.error("Runetick sign-in from browser did not work", e);
			session.unlink();
			state.setLinked(false);
			panel.showState(false, "Sign-in did not work; try again");
			throw e;
		}
	}

	private void disconnect()
	{
		executor.submit(() ->
		{
			try
			{
				stopLinkServer();
				session.unlink();
				setLinked(false);
			}
			catch (IOException | RuntimeException e)
			{
				log.error("Could not disconnect from Runetick", e);
			}
		});
	}

	private synchronized void setLinkServer(LinkServer server)
	{
		linkServer = server;
	}

	private synchronized void stopLinkServer()
	{
		if (linkServer != null)
		{
			linkServer.stop();
			linkServer = null;
		}
	}
}
