package com.runetick;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A small mark on the item icon in each GE slot: check hold, plus-minus reprice, cross pull.
 * Under it, the estimated profit after tax and the time left, and a total under the window.
 */
public class GeSlotOverlay extends Overlay
{
	private static final Color REPRICE = new Color(0xf4, 0xf4, 0xf5);
	private static final Color PULL = new Color(0xef, 0x44, 0x44);
	private static final Color PLATE = new Color(5, 5, 6, 230);
	private static final Color INK = new Color(0xf4, 0xf4, 0xf5);
	private static final Font GLYPH = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final Font LEGEND = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

	private final Client client;
	private final RuneTickConfig config;
	private final OfferGuard offerGuard;
	private final GeHistory geHistory;
	private long totalGp;
	private int totalSlots;

	@Inject
	public GeSlotOverlay(Client client, RuneTickConfig config, OfferGuard offerGuard, GeHistory geHistory)
	{
		this.client = client;
		this.config = config;
		this.offerGuard = offerGuard;
		this.geHistory = geHistory;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			if (!config.showGeTips() || GeView.offerScreenOpen(client))
			{
				return null;
			}
			GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
			if (offers == null)
			{
				return null;
			}
			totalGp = 0;
			totalSlots = 0;
			for (int slot = 0; slot < offers.length && slot < 8; slot++)
			{
				drawSlot(graphics, slot, offers[slot]);
			}
			drawLegend(graphics);
		}
		catch (Throwable t)
		{
			return null;
		}
		return null;
	}

	private void drawSlot(Graphics2D graphics, int slot, GrandExchangeOffer offer)
	{
		if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY || offer.getItemId() <= 0)
		{
			return;
		}
		Widget widget = client.getWidget(InterfaceID.GE_OFFERS, 7 + slot);
		if (widget == null || widget.isHidden())
		{
			return;
		}
		Rectangle slotBounds = widget.getBounds();
		if (slotBounds == null || slotBounds.width < 16)
		{
			return;
		}
		boolean buy = offer.getState() == GrandExchangeOfferState.BUYING
			|| offer.getState() == GrandExchangeOfferState.BOUGHT
			|| offer.getState() == GrandExchangeOfferState.CANCELLED_BUY;
		String glyph = "\u2713";
		Color glyphColor = Accent.color();
		RuneTickApi.ItemQuote quote = offerGuard.quoteOf(offer.getItemId());
		if (quote != null)
		{
			int pct = config.offerAlertPercent();
			if (pct < 1) pct = 2;
			OfferAlert.Kind kind = OfferAlert.classify(
				buy, offer.getPrice(), quote.getBuy(), quote.getSell(), quote.getAfterTaxSell(), 0,
				pct, config.offerMarginFloor());
			if (kind == OfferAlert.Kind.BUY_RAISE || kind == OfferAlert.Kind.SELL_LOWER)
			{
				glyph = "\u00b1";
				glyphColor = REPRICE;
			}
			else if (kind == OfferAlert.Kind.MARGIN_CRUSH)
			{
				glyph = "\u2715";
				glyphColor = PULL;
			}
		}

		int badge = 16;
		int x = slotBounds.x + slotBounds.width - badge - 3;
		int y = slotBounds.y + 3;
		graphics.setColor(PLATE);
		graphics.fillRoundRect(x - 1, y - 1, badge + 2, badge + 2, 6, 6);
		graphics.setColor(glyphColor);
		graphics.drawRoundRect(x - 1, y - 1, badge + 2, badge + 2, 6, 6);
		graphics.setFont(GLYPH);
		FontMetrics fm = graphics.getFontMetrics();
		int tx = x + (badge - fm.stringWidth(glyph)) / 2;
		int ty = y + (badge + fm.getAscent() - fm.getDescent()) / 2 + 1;
		graphics.drawString(glyph, tx, ty);

		if (config.showSlotProfit())
		{
			drawProfit(graphics, slotBounds, offer, buy, quote);
		}
	}

	private SlotProfit estimate(GrandExchangeOffer offer, boolean buy, RuneTickApi.ItemQuote quote)
	{
		int total = Math.max(0, offer.getTotalQuantity());
		if (buy)
		{
			return quote == null ? null : SlotProfit.forBuy(total, offer.getPrice(), quote.getAfterTaxSell());
		}
		CostBasis.Open paid = CostBasis.openBuys(geHistory.all(), offer.getItemId());
		return paid == null ? null : SlotProfit.forSell(total, offer.getPrice(), paid.each);
	}

	private void drawProfit(Graphics2D graphics, Rectangle slot, GrandExchangeOffer offer, boolean buy, RuneTickApi.ItemQuote quote)
	{
		SlotProfit est = estimate(offer, buy, quote);
		int remaining = Math.max(0, offer.getTotalQuantity() - offer.getQuantitySold());
		boolean live = offer.getState() == GrandExchangeOfferState.BUYING || offer.getState() == GrandExchangeOfferState.SELLING;
		String eta = live && remaining > 0 ? offerGuard.etaFor(offer.getItemId(), remaining, buy, offer.getPrice()) : "";
		if (est == null && (eta == null || eta.isEmpty()))
		{
			return;
		}
		if (est != null)
		{
			totalGp += est.gp;
			totalSlots++;
		}
		String money = est == null ? "" : est.text();
		String rest = eta == null || eta.isEmpty() ? "" : (money.isEmpty() ? eta : " \u00b7 " + eta);
		graphics.setFont(LEGEND);
		FontMetrics fm = graphics.getFontMetrics();
		int width = fm.stringWidth(money + rest) + 8;
		int height = fm.getHeight() + 2;
		int x = slot.x + 3;
		int y = slot.y + 3;
		graphics.setColor(PLATE);
		graphics.fillRoundRect(x, y, width, height, 6, 6);
		int baseline = y + fm.getAscent() + 1;
		if (!money.isEmpty())
		{
			graphics.setColor(est.gp < 0 ? PULL : Accent.color());
			graphics.drawString(money, x + 4, baseline);
		}
		graphics.setColor(INK);
		graphics.drawString(rest, x + 4 + fm.stringWidth(money), baseline);
	}

	/** One line in the top-right of the Grand Exchange window. */
	private void drawLegend(Graphics2D graphics)
	{
		Widget window = client.getWidget(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER);
		if (window == null || window.isHidden())
		{
			window = client.getWidget(InterfaceID.GE_OFFERS, 0);
		}
		if (window == null || window.isHidden())
		{
			return;
		}
		Rectangle bounds = window.getBounds();
		if (bounds == null || bounds.width < 80)
		{
			return;
		}
		String[] glyphs = {"\u2713", "\u00b1", "\u2715"};
		String[] words = {"hold", "reprice", "pull"};
		Color[] colors = {Accent.color(), INK, PULL};
		graphics.setFont(LEGEND);
		FontMetrics fm = graphics.getFontMetrics();
		int pad = 6;
		int gap = 10;
		int width = pad * 2;
		for (int i = 0; i < glyphs.length; i++)
		{
			width += fm.stringWidth(glyphs[i] + " " + words[i]);
			if (i < glyphs.length - 1)
			{
				width += gap;
			}
		}
		if (config.showSlotProfit() && totalSlots > 0)
		{
			String sum = "open offers " + (totalGp > 0 ? "+" : totalGp < 0 ? "-" : "") + BankView.compact(Math.abs(totalGp)) + " after tax";
			width += gap + fm.stringWidth(sum);
		}
		int height = fm.getHeight() + 6;
		int x = bounds.x + 8;
		int y = bounds.y + bounds.height + 3;
		if (y + height > client.getCanvasHeight() - 2)
		{
			y = Math.max(2, bounds.y - height - 3);
		}
		graphics.setColor(PLATE);
		graphics.fillRoundRect(x, y, width, height, 8, 8);
		int cursor = x + pad;
		int baseline = y + fm.getAscent() + 3;
		for (int i = 0; i < glyphs.length; i++)
		{
			String bit = glyphs[i] + " " + words[i];
			graphics.setColor(colors[i]);
			graphics.drawString(bit, cursor, baseline);
			cursor += fm.stringWidth(bit) + gap;
		}
		if (config.showSlotProfit() && totalSlots > 0)
		{
			String sign = totalGp > 0 ? "+" : totalGp < 0 ? "-" : "";
			graphics.setColor(INK);
			graphics.drawString("open offers ", cursor, baseline);
			cursor += fm.stringWidth("open offers ");
			graphics.setColor(totalGp < 0 ? PULL : Accent.color());
			String amount = sign + BankView.compact(Math.abs(totalGp));
			graphics.drawString(amount, cursor, baseline);
			cursor += fm.stringWidth(amount);
			graphics.setColor(INK);
			graphics.drawString(" after tax", cursor, baseline);
		}
	}
}
