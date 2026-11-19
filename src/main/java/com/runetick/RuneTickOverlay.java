package com.runetick;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Optional tiny status chip. The full board lives in the sidebar panel; GE item tips use
 * {@link GeOfferOverlay}.
 */
public class RuneTickOverlay extends OverlayPanel
{
	private static final Color DONE = new Color(0x00, 0xc8, 0x05);
	private static final Color MUTED = new Color(0x9a, 0x9a, 0x9a);
	private static final Color BUY = new Color(0x4d, 0x9b, 0xff);
	private static final Color TITLE = new Color(0xe8, 0xe8, 0xe8);

	private final Client client;
	private final RuneTickConfig config;
	private final RuneTickState state;

	@Inject
	public RuneTickOverlay(Client client, RuneTickConfig config, RuneTickState state)
	{
		this.client = client;
		this.config = config;
		this.state = state;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setMovable(true);
		setSnappable(true);
		getPanelComponent().setPreferredSize(new Dimension(190, 0));
		getPanelComponent().setBorder(new java.awt.Rectangle(3, 3, 3, 3));
		getPanelComponent().setGap(new java.awt.Point(0, 1));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			if (!config.showStatusHud() || client.getGameState() != GameState.LOGGED_IN)
			{
				return null;
			}
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Runetick")
				.color(TITLE)
				.build());
			String left = !state.isLinked() ? "not linked"
				: state.isSyncing() ? "syncing..."
				: state.isLoadingBoard() ? "loading..."
				: "ok";
			String right;
			if (config.showCash())
			{
				long known = state.getInventoryGp() + (state.isBankSeen() ? state.getBankGp() : 0);
				right = PriceFormat.gpCompact(known);
			}
			else
			{
				right = "Alt-drag";
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left(left)
				.right(right)
				.leftColor(state.isLinked() ? DONE : MUTED)
				.rightColor(BUY)
				.build());
			return super.render(graphics);
		}
		catch (Throwable t)
		{
			return null;
		}
	}
}
