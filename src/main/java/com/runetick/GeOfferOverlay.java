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
	private static final Color INK = new Color(0xf4, 0xf4, 0xf5);
	private static final Color GAIN = new Color(0x4a, 0xde, 0x80);
	private static final Color MUTED = new Color(0xb0, 0xb0, 0xb0);
	private static final Color WARN = new Color(0xef, 0x44, 0x44);

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
		getPanelComponent().setPreferredSize(new Dimension(220, 0));
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

		int itemId = GeView.tipItemId(client);
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
		row("Slots", (8 - free) + " busy | " + free + " free", MUTED, INK);

		if (config.showCash())
		{
			String bank = state.isBankSeen() ? PriceFormat.gp(state.getBankGp()) : "-";
			row("Cash", "inv " + PriceFormat.gp(state.getInventoryGp()) + " | bank " + bank, MUTED, INK);
		}

		if (config.showFatAndDips())
		{
			List<RuneTickApi.FatSpread> fat = state.getFat();
			if (!fat.isEmpty())
			{
				RuneTickApi.FatSpread f = fat.get(0);
				row("Margin", PriceFormat.abbreviate(f.getName(), 20), MUTED, INK);
				row(FocusCopy.fatPrices(f), PriceFormat.gpCompact(f.getDreamGp()), INK, GAIN);
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
		GrandExchangeOffer offer = offerFor(realId);
		long buy = quote.getBuy();
		long sell = quote.getSell();
		if (offer != null && offer.getPrice() > 0)
		{
			if (buying(offer))
			{
				buy = offer.getPrice();
			}
			else
			{
				sell = offer.getPrice();
			}
		}

		title(PriceFormat.abbreviate(quote.getName(), 28));
		row("Buy", PriceFormat.gpExact(buy), MUTED, INK);
		row("Sell", PriceFormat.gpExact(sell), MUTED, INK);

		long each = FocusCopy.eachProfit(buy, sell);
		if (each != Long.MIN_VALUE)
		{
			row("Each", signed(each), MUTED, each >= 0 ? GAIN : WARN);
			int offerQty = offer != null ? offer.getTotalQuantity() : 0;
			if (offerQty > 0)
			{
				row("On offer", signed(each * offerQty), MUTED, each >= 0 ? GAIN : WARN);
			}
		}

		long cash = state.getInventoryGp() + (state.isBankSeen() ? state.getBankGp() : 0);
		int limit = quote.getLimit();
		long afford = Afford.units(cash, buy, limit);
		String affordText = limit > 0
			? PriceFormat.gp(afford) + " / " + PriceFormat.gp(limit)
			: PriceFormat.gp(afford);
		if (!state.isBankSeen())
		{
			affordText = affordText + " inv";
		}
		row("Afford", affordText, MUTED, INK);
		if (each != Long.MIN_VALUE)
		{
			row("Potential", signed(each * afford), MUTED, each >= 0 ? GAIN : WARN);
		}
		if (offer != null && offer.getTotalQuantity() > 0)
		{
			row("Filled", offer.getQuantitySold() + " / " + offer.getTotalQuantity(), MUTED, MUTED);
		}

		addOfferEta(realId, quote);

		return super.render(graphics);
	}

	private static String signed(long gp)
	{
		return (gp > 0 ? "+" : "") + PriceFormat.gpExact(gp);
	}

	private GrandExchangeOffer offerFor(int itemId)
	{
		GrandExchangeOffer selected = offerAt(GeView.selectedSlot(client));
		if (selected != null && selected.getItemId() == itemId)
		{
			return selected;
		}
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		if (offers == null)
		{
			return null;
		}
		for (GrandExchangeOffer offer : offers)
		{
			if (offer != null && offer.getItemId() == itemId && offer.getState() != GrandExchangeOfferState.EMPTY)
			{
				return offer;
			}
		}
		return null;
	}

	private GrandExchangeOffer offerAt(int slot)
	{
		if (slot < 0)
		{
			return null;
		}
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		if (offers == null || slot >= offers.length)
		{
			return null;
		}
		GrandExchangeOffer offer = offers[slot];
		if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY || offer.getItemId() <= 0)
		{
			return null;
		}
		return offer;
	}

	private static boolean buying(GrandExchangeOffer offer)
	{
		GrandExchangeOfferState state = offer.getState();
		return state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;
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
			row("Est. fill", eta + (vol > 0 ? " · vol " + PriceFormat.gp(vol) + "/5m" : ""), MUTED, INK);
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

	private void title(String text)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(HudText.ascii(text))
			.color(Accent.color())
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
