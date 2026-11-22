package com.runetick;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Tags the player's own items: open flips with their price, held Grand Exchange buys with
 * how the sell compares to what was paid, and, in a bank view, stack values with the rest dimmed.
 */
public class ItemTipOverlay extends WidgetItemOverlay
{
	private static final Color TAG = new Color(0x00, 0xc8, 0x05);
	private static final Color DOWN = new Color(0xf8, 0x71, 0x71);
	private static final Color PLAIN = new Color(0xf4, 0xf4, 0xf5);
	private static final Color DIM = new Color(0, 0, 0, 150);

	private final RuneTickConfig config;
	private final RuneTickState state;
	private final BankHoldings bankHoldings;
	private final InventoryHoldings inventoryHoldings;

	@Inject
	public ItemTipOverlay(RuneTickConfig config, RuneTickState state, BankHoldings bankHoldings, InventoryHoldings inventoryHoldings)
	{
		this.config = config;
		this.state = state;
		this.bankHoldings = bankHoldings;
		this.inventoryHoldings = inventoryHoldings;
		// Inv + bank + GE inventory sidebar — avoid the full GE offer widgets (they can blow up).
		showOnInventory();
		showOnBank();
		showOnInterfaces(InterfaceID.GRAND_EXCHANGE_INVENTORY);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		try
		{
			if (widgetItem == null || itemId <= 0)
			{
				return;
			}
			Rectangle bounds = widgetItem.getCanvasBounds();
			if (bounds == null)
			{
				return;
			}
			// Never call ItemManager here. Canonicalize asserts under -ea and a missed catch
			// used to take down the whole client during GE / bank PIN. The ids were resolved
			// on the client thread when the container was read.
			boolean inBank = inBank(widgetItem);
			int id = inBank ? bankHoldings.canonicalOf(itemId) : inventoryHoldings.canonicalOf(itemId);
			if (id <= 0)
			{
				id = itemId;
			}

			BankView.Mode mode = inBank ? config.bankView() : BankView.Mode.OFF;
			if (mode != BankView.Mode.OFF)
			{
				BankView.Mark mark = (mode == BankView.Mode.VALUE ? state.getValueMarks() : state.getProfitMarks()).get(id);
				if (mark == null)
				{
					graphics.setColor(DIM);
					graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
					return;
				}
				draw(graphics, bounds, mark.label, toneColor(mark.tone));
				return;
			}

			if (!config.showItemHighlights())
			{
				return;
			}
			String flip = state.isLinked() ? state.highlightLabels().get(id) : null;
			if (flip != null)
			{
				draw(graphics, bounds, flip, TAG);
				return;
			}
			BankView.Mark held = state.getProfitMarks().get(id);
			if (held != null)
			{
				draw(graphics, bounds, held.label, toneColor(held.tone));
			}
		}
		catch (Throwable t)
		{
			// AssertionError is an Error, not a RuntimeException — must catch Throwable.
		}
	}

	private static Color toneColor(int tone)
	{
		return tone > 0 ? TAG : tone < 0 ? DOWN : PLAIN;
	}

	private static boolean inBank(WidgetItem widgetItem)
	{
		Widget widget = widgetItem.getWidget();
		return widget != null && (widget.getId() >>> 16) == InterfaceID.BANK;
	}

	private static void draw(Graphics2D graphics, Rectangle bounds, String label, Color color)
	{
		TextComponent text = new TextComponent();
		text.setText(label);
		text.setColor(color);
		text.setOutline(true);
		text.setFont(FontManager.getRunescapeSmallFont());
		text.setPosition(new Point(bounds.x, bounds.y + Math.max(0, bounds.height - 2)));
		text.render(graphics);
	}
}
