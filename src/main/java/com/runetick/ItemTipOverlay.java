package com.runetick;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Tags inventory / GE inventory items that are open flips or recommended buys.
 */
public class ItemTipOverlay extends WidgetItemOverlay
{
	private static final Color TAG = new Color(0x00, 0xc8, 0x05);

	private final RuneTickConfig config;
	private final RuneTickState state;

	@Inject
	public ItemTipOverlay(RuneTickConfig config, RuneTickState state)
	{
		this.config = config;
		this.state = state;
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
			if (!config.showItemHighlights() || !state.isLinked() || widgetItem == null || itemId <= 0)
			{
				return;
			}

			Map<Integer, String> labels = state.highlightLabels();
			if (labels.isEmpty())
			{
				return;
			}
			// Exact id only — never call ItemManager here. Canonicalize asserts under -ea
			// and a missed catch used to take down the whole client during GE / bank PIN.
			String label = labels.get(itemId);
			if (label == null)
			{
				return;
			}

			Rectangle bounds = widgetItem.getCanvasBounds();
			if (bounds == null)
			{
				return;
			}

			TextComponent text = new TextComponent();
			text.setText(label);
			text.setColor(TAG);
			text.setOutline(true);
			text.setFont(FontManager.getRunescapeSmallFont());
			text.setPosition(new Point(bounds.x, bounds.y + Math.max(0, bounds.height - 2)));
			text.render(graphics);
		}
		catch (Throwable t)
		{
			// AssertionError is an Error, not a RuntimeException — must catch Throwable.
		}
	}
}
