package com.runetick;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Compact GE tip: hero buy/sell first, fat/dip context, market second.
 */
public class GeOfferOverlay extends OverlayPanel
{
	private static final Color BUY = new Color(0x4d, 0x9b, 0xff);
	private static final Color SELL = new Color(0xff, 0xb0, 0x3a);
	private static final Color GOOD = new Color(0x00, 0xc8, 0x05);
	private static final Color MUTED = new Color(0xb0, 0xb0, 0xb0);
	private static final Color WARN = new Color(0xff, 0x66, 0x66);
	private static final Color TITLE = new Color(0xe8, 0xe8, 0xe8);

	private final Client client;
	private final RuneTickConfig config;
	private final RuneTickState state;
	private final OfferGuard offerGuard;

	@Inject
	public GeOfferOverlay(Client client, RuneTickConfig config, RuneTickState state, OfferGuard offerGuard)
	{
		this.client = client;
		this.config = config;
		this.state = state;
		this.offerGuard = offerGuard;
		setPosition(OverlayPosition.TOP_RIGHT);
		setPriority(PRIORITY_HIGH);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setMovable(true);
		setSnappable(true);
		getPanelComponent().setPreferredSize(new Dimension(300, 0));
		getPanelComponent().setBorder(new java.awt.Rectangle(4, 4, 4, 4));
		getPanelComponent().setGap(new java.awt.Point(0, 2));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			return renderSafe(graphics);
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	private Dimension renderSafe(Graphics2D graphics)
	{
		if (!config.showGeTips() || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		Widget pin = client.getWidget(InterfaceID.BANK_PIN, 0);
		if (pin != null && !pin.isHidden())
		{
			return null;
		}
		if (!geOpen())
		{
			return null;
		}

		int itemId = client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
		return itemId <= 0 ? renderOverview(graphics) : renderItemTip(graphics, itemId);
	}

	private boolean geOpen()
	{
		Widget window = client.getWidget(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER);
		if (window != null && !window.isHidden())
		{
			return true;
		}
		Widget offer = client.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER);
		if (offer != null && !offer.isHidden())
		{
			return true;
		}
		Widget root = client.getWidget(InterfaceID.GRAND_EXCHANGE, 0);
		return root != null && !root.isHidden();
	}

	private Dimension renderOverview(Graphics2D graphics)
	{
		int free = countFreeSlots();
		title("GE tip");
		row("Slots", (8 - free) + " busy | " + free + " free", MUTED, GOOD);

		if (config.showCash())
		{
			String bank = state.isBankSeen() ? PriceFormat.gp(state.getBankGp()) : "-";
			row("Cash", "inv " + PriceFormat.gp(state.getInventoryGp()) + " | bank " + bank, MUTED, GOOD);
		}

		if (config.showFatAndDips())
		{
			List<RuneTickApi.FatSpread> fat = state.getFat();
			if (!fat.isEmpty())
			{
				RuneTickApi.FatSpread f = fat.get(0);
				row("FAT", PriceFormat.abbreviate(f.getName(), 20), SELL, SELL);
				row(FocusCopy.fatPrices(f), PriceFormat.gpCompact(f.getDreamGp()), BUY, GOOD);
			}
			List<RuneTickApi.Dip> dips = state.getDips();
			if (!dips.isEmpty())
			{
				RuneTickApi.Dip d = dips.get(0);
				row("DIP", PriceFormat.abbreviate(d.getName(), 18) + " " + FocusCopy.dipPct(d), BUY, BUY);
				row(FocusCopy.dipPrices(d), "", GOOD, MUTED);
			}
		}

		return super.render(graphics);
	}

	private Dimension renderItemTip(Graphics2D graphics, int itemId)
	{
		RuneTickApi.ItemQuote quote = state.getGeItem();
		if (quote == null)
		{
			title("GE tip");
			row("Loading tip...", "", MUTED, MUTED);
			return super.render(graphics);
		}

		int realId = state.getGeItemId() > 0 ? state.getGeItemId() : itemId;
		boolean selling = client.getVarbitValue(Varbits.GE_OFFER_CREATION_TYPE) == 1;
		RuneTickApi.PlacedFlip placed = state.placedFor(realId);
		RuneTickApi.PlanPosition rec = state.recommendationFor(realId);
		RuneTickApi.FatSpread fat = config.showFatAndDips() ? state.fatFor(realId) : null;
		RuneTickApi.Dip dip = config.showFatAndDips() ? state.dipFor(realId) : null;

		long recBuy;
		long recSell;
		String tag = selling ? "SELL" : "BUY";
		if (placed != null)
		{
			recBuy = placed.getBid();
			recSell = placed.getAsk();
			tag = "FLIP";
		}
		else if (fat != null)
		{
			recBuy = fat.getBid();
			recSell = fat.getAsk();
			tag = "FAT";
		}
		else if (dip != null)
		{
			recBuy = dip.getPrice();
			recSell = FocusCopy.dipAsk(dip.getPrice());
			tag = "DIP";
		}
		else if (rec != null)
		{
			recBuy = rec.getBid();
			recSell = rec.getAsk();
		}
		else
		{
			recBuy = quote.getBuy();
			recSell = quote.getSell();
		}

		title(PriceFormat.abbreviate(quote.getName(), 28));
		row(tag, selling ? "selling" : "buying", selling ? SELL : BUY, MUTED);

		if (selling)
		{
			row("Sell at", PriceFormat.gpExact(recSell), SELL, SELL);
			row("Mkt", PriceFormat.gpExact(quote.getSell()), MUTED, MUTED);
			if (quote.getAfterTaxSell() > 0)
			{
				row("After tax", PriceFormat.gpExact(quote.getAfterTaxSell()), MUTED, MUTED);
			}
			if (placed != null && placed.getBid() > 0)
			{
				long margin = quote.getAfterTaxSell() > 0
					? quote.getAfterTaxSell() - placed.getBid()
					: recSell - placed.getBid();
				row("vs buy", (margin >= 0 ? "+" : "") + PriceFormat.gpExact(margin),
					MUTED, margin >= 0 ? GOOD : WARN);
			}
		}
		else
		{
			row("Buy at", PriceFormat.gpExact(recBuy), BUY, BUY);
			row("Then sell", PriceFormat.gpExact(recSell), SELL, SELL);
			row("Mkt", PriceFormat.gp(quote.getBuy()) + " / " + PriceFormat.gp(quote.getSell()), MUTED, MUTED);
			addAffordRows(recBuy > 0 ? recBuy : quote.getBuy(), quote.getLimit());
		}

		if (fat != null)
		{
			row("Dream", PriceFormat.gpCompact(fat.getDreamGp()) + " | " + PriceFormat.gp(fat.getMarginPerUnit()) + "/ea",
				MUTED, GOOD);
		}
		else if (dip != null)
		{
			row("Dip", FocusCopy.dipPct(dip) + " | usual " + PriceFormat.gp(dip.getMedian30()), BUY, MUTED);
		}
		else if (placed != null)
		{
			row("Progress", FocusCopy.flipLine(placed), MUTED, BUY);
		}
		else if (rec != null)
		{
			row("Plan", rec.getUnits() + " | " + PriceFormat.gpCompact(rec.getGpPerHour()) + "/h", MUTED, GOOD);
		}

		if (selling && quote.getLimit() > 0)
		{
			String market = "";
			if (quote.getAnalytics() != null && quote.getAnalytics().getMarket() != null
				&& quote.getAnalytics().getMarket().getLabel() != null)
			{
				market = quote.getAnalytics().getMarket().getLabel();
			}
			row("Limit " + PriceFormat.gp(quote.getLimit()), market, MUTED, MUTED);
		}

		addOfferEta(realId, quote);

		return super.render(graphics);
	}

	/** Show volume-based ETA when a matching open GE offer (or placed flip) has remaining qty. */
	private void addOfferEta(int itemId, RuneTickApi.ItemQuote quote)
	{
		if (!config.offerEta())
		{
			return;
		}
		int remaining = remainingForItem(itemId);
		if (remaining <= 0)
		{
			return;
		}
		long vol = quote != null ? Math.max(0L, quote.getVolume()) : offerGuard.volumeOf(itemId);
		String eta = OfferEta.format(remaining, vol);
		if (!eta.isEmpty())
		{
			row("Est. fill", eta + (vol > 0 ? " · vol " + PriceFormat.gp(vol) + "/5m" : ""), MUTED, GOOD);
		}
	}

	private int remainingForItem(int itemId)
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		if (offers != null)
		{
			for (GrandExchangeOffer offer : offers)
			{
				if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY)
				{
					continue;
				}
				if (offer.getItemId() == itemId)
				{
					return Math.max(0, offer.getTotalQuantity() - offer.getQuantitySold());
				}
			}
		}
		RuneTickApi.PlacedFlip placed = state.placedFor(itemId);
		if (placed != null)
		{
			if ("selling".equals(placed.getStatus()))
			{
				return Math.max(0, placed.getBought() - placed.getSold());
			}
			return Math.max(0, placed.getUnits() - placed.getBought());
		}
		return 0;
	}

	/** Units you can afford at the tip buy price from inv + last-known bank, vs GE limit. */
	private void addAffordRows(long buyPrice, int limit)
	{
		long inv = state.getInventoryGp();
		long bank = state.isBankSeen() ? state.getBankGp() : 0;
		long cash = inv + bank;
		long can = Afford.units(cash, buyPrice, limit);
		long affordOnly = Afford.units(cash, buyPrice, 0);

		String cashRight = state.isBankSeen()
			? "inv " + PriceFormat.gpCompact(inv) + " + bank " + PriceFormat.gpCompact(bank)
			: "inv " + PriceFormat.gpCompact(inv) + " (bank ?)";
		row("Cash", cashRight, MUTED, GOOD);

		if (buyPrice <= 0)
		{
			return;
		}
		if (limit > 0)
		{
			row("Can buy", PriceFormat.gp(can) + " / lim " + PriceFormat.gp(limit), BUY, MUTED);
			if (affordOnly > limit)
			{
				row("Afford", PriceFormat.gp(affordOnly) + " (cash)", MUTED, MUTED);
			}
		}
		else
		{
			row("Can buy", PriceFormat.gp(can) + " (cash)", BUY, GOOD);
		}
	}

	private void title(String text)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(HudText.ascii(text))
			.color(TITLE)
			.build());
	}

	private void row(String left, String right, Color leftColor, Color rightColor)
	{
		LineComponent.LineComponentBuilder b = LineComponent.builder()
			.left(HudText.ascii(left))
			.leftColor(leftColor);
		if (right != null && !right.isEmpty())
		{
			b.right(HudText.ascii(right)).rightColor(rightColor);
		}
		panelComponent.getChildren().add(b.build());
	}

	private int countFreeSlots()
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		if (offers == null)
		{
			return 0;
		}
		int free = 0;
		for (GrandExchangeOffer offer : offers)
		{
			if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY)
			{
				free++;
			}
		}
		return free;
	}
}
