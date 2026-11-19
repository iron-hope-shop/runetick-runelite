package com.runetick;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar cockpit: compact wealth tiles + card rows (width-capped — no horizontal overflow).
 * Click / right-click a row to open that item on Runetick.
 */
public class RuneTickPanel extends PluginPanel
{
	private static final Color ACCENT = new Color(0x00, 0xc8, 0x05);
	private static final Color CARD = new Color(0x2a, 0x2a, 0x2a);
	private static final Color CARD_LINE = new Color(0x3a, 0x3a, 0x3a);
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;

	private final JLabel status = plain(FontManager.getRunescapeBoldFont(), Color.WHITE);
	private final JLabel sync = plain(FontManager.getRunescapeSmallFont(), MUTED);
	private final JLabel detail = plain(FontManager.getRunescapeSmallFont(), MUTED);
	private final JLabel kpiInvGe = kpiValue();
	private final JLabel kpiNet = kpiValue();
	private final JLabel kpiSession = kpiValue();
	private final JLabel kpiMeta = plain(FontManager.getRunescapeSmallFont(), MUTED);
	private final JPanel bankHost = boardHost();
	private final JPanel liveHost = boardHost();
	private final JPanel fatHost = boardHost();
	private final JPanel dipHost = boardHost();
	private final JPanel flipHost = boardHost();
	private final JPanel histHost = boardHost();
	private final JPanel col = new JPanel();
	private final JButton connectBtn = new JButton();
	private final JButton siteBtn = new JButton("Open site");

	private boolean linked;
	private final Runnable onConnect;
	private final Runnable onDisconnect;
	private final Runnable onOpenSite;
	private final IntConsumer onOpenItem;

	public RuneTickPanel(Runnable onConnect, Runnable onDisconnect, Runnable onOpenSite, IntConsumer onOpenItem)
	{
		this.onConnect = onConnect;
		this.onDisconnect = onDisconnect;
		this.onOpenSite = onOpenSite;
		this.onOpenItem = onOpenItem == null ? id -> { } : onOpenItem;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(6, 6, 6, 6));
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				capColumnWidth();
				reflowHosts();
			}
		});

		JLabel title = plain(FontManager.getRunescapeBoldFont(), Color.WHITE);
		title.setText("Runetick");

		connectBtn.setFocusPainted(false);
		connectBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		connectBtn.addActionListener(e ->
		{
			try
			{
				(linked ? onDisconnect : onConnect).run();
			}
			catch (Throwable t)
			{
				showMessage("Something went wrong - see error.log");
			}
		});

		siteBtn.setFocusPainted(false);
		siteBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		siteBtn.addActionListener(e ->
		{
			try
			{
				onOpenSite.run();
			}
			catch (Throwable t)
			{
				showMessage("Could not open the site");
			}
		});

		JPanel actions = new JPanel(new GridLayout(1, 2, 4, 0));
		actions.setOpaque(false);
		actions.setAlignmentX(Component.LEFT_ALIGNMENT);
		actions.setMaximumSize(new Dimension(columnWidth(), 28));
		actions.add(connectBtn);
		actions.add(siteBtn);

		JPanel wealth = wealthStrip();

		col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
		col.setOpaque(false);
		col.setAlignmentX(Component.LEFT_ALIGNMENT);
		capColumnWidth();

		stretch(title);
		stretch(status);
		stretch(sync);
		stretch(detail);
		stretch(kpiMeta);

		col.add(title);
		col.add(Box.createVerticalStrut(4));
		col.add(actions);
		col.add(Box.createVerticalStrut(6));
		col.add(status);
		col.add(sync);
		col.add(detail);
		col.add(Box.createVerticalStrut(8));
		col.add(section("WEALTH"));
		col.add(wealth);
		col.add(Box.createVerticalStrut(2));
		col.add(kpiMeta);
		col.add(Box.createVerticalStrut(10));
		col.add(section("LIVE GE"));
		col.add(liveHost);
		col.add(Box.createVerticalStrut(8));
		col.add(section("FAT"));
		col.add(fatHost);
		col.add(Box.createVerticalStrut(8));
		col.add(section("FLIPS"));
		col.add(flipHost);
		col.add(Box.createVerticalStrut(8));
		col.add(section("BANK HITS"));
		col.add(bankHost);
		col.add(Box.createVerticalStrut(8));
		col.add(section("DIPS"));
		col.add(dipHost);
		col.add(Box.createVerticalStrut(8));
		col.add(section("RECENT"));
		col.add(histHost);
		col.add(Box.createVerticalStrut(6));
		JLabel hint = plain(FontManager.getRunescapeSmallFont(), ColorScheme.MEDIUM_GRAY_COLOR);
		hint.setText(wrap("Click a card to open on Runetick. Open bank once to scan."));
		stretch(hint);
		col.add(hint);

		JScrollPane scroll = new JScrollPane(col);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(scroll, BorderLayout.CENTER);

		showState(false, "");
		showSnapshot(Snapshot.empty());
	}

	private void capColumnWidth()
	{
		int w = columnWidth();
		// Cap WIDTH only — never pin preferred HEIGHT (that collapses the sidebar to empty).
		col.setMaximumSize(new Dimension(w, Integer.MAX_VALUE));
	}

	private int columnWidth()
	{
		int outer = getWidth() > 40 ? getWidth() : PANEL_WIDTH;
		return Math.max(160, Math.min(PANEL_WIDTH - 8, outer - 12));
	}

	private int contentWidth()
	{
		return Math.max(140, columnWidth() - 12);
	}

	private JPanel wealthStrip()
	{
		JPanel strip = new JPanel(new GridLayout(1, 3, 4, 0));
		strip.setOpaque(false);
		strip.setAlignmentX(Component.LEFT_ALIGNMENT);
		strip.setMaximumSize(new Dimension(columnWidth(), 52));
		strip.setPreferredSize(new Dimension(columnWidth(), 52));
		strip.add(kpiTile("INV+GE", kpiInvGe));
		strip.add(kpiTile("NET", kpiNet));
		strip.add(kpiTile("SESSION", kpiSession));
		return strip;
	}

	private static JPanel kpiTile(String label, JLabel value)
	{
		JPanel tile = new JPanel();
		tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
		tile.setBackground(CARD);
		tile.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_LINE),
			new EmptyBorder(4, 4, 4, 4)));
		JLabel lab = plain(FontManager.getRunescapeSmallFont(), ColorScheme.BRAND_ORANGE);
		lab.setText(label);
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
		tile.add(lab);
		tile.add(value);
		return tile;
	}

	private static JLabel kpiValue()
	{
		JLabel l = plain(FontManager.getRunescapeBoldFont(), Color.WHITE);
		l.setText("-");
		return l;
	}

	private static JPanel boardHost()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	private void stretch(JLabel l)
	{
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	private String wrap(String text)
	{
		return "<html><body style='width:" + contentWidth() + "px'>" + escape(HudText.ascii(text)) + "</body></html>";
	}

	private void reflowHosts()
	{
		capColumnWidth();
		int w = columnWidth();
		for (JPanel host : new JPanel[]{liveHost, bankHost, fatHost, dipHost, flipHost, histHost})
		{
			host.setMaximumSize(new Dimension(w, Integer.MAX_VALUE));
			for (Component c : host.getComponents())
			{
				if (c instanceof JPanel)
				{
					int h = Math.max(36, c.getPreferredSize().height);
					c.setMaximumSize(new Dimension(w, h));
				}
			}
		}
		fitWrapped(detail);
		fitWrapped(kpiMeta);
		fitWrapped(sync);
		revalidate();
	}

	private void fitWrapped(JLabel l)
	{
		String raw = l.getText();
		if (raw == null || !raw.startsWith("<html>"))
		{
			return;
		}
		String plain = raw.replaceAll("<[^>]+>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
		l.setText(wrap(plain));
		l.setPreferredSize(null);
		Dimension pref = l.getPreferredSize();
		l.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(16, pref.height + 2)));
	}

	private static JLabel plain(Font font, Color color)
	{
		JLabel l = new JLabel();
		l.setFont(font);
		l.setForeground(color);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		l.setVerticalAlignment(SwingConstants.TOP);
		return l;
	}

	private static JLabel section(String text)
	{
		JLabel l = plain(FontManager.getRunescapeSmallFont(), ColorScheme.BRAND_ORANGE);
		l.setText(text);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	public void showState(boolean isLinked, String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			linked = isLinked;
			status.setText(isLinked ? "Connected" : "Not connected");
			status.setForeground(isLinked ? ACCENT : MUTED);
			connectBtn.setText(isLinked ? "Disconnect" : "Connect");
			if (message != null && !message.isEmpty())
			{
				showMessage(message);
			}
			else if (!isLinked)
			{
				showMessage("Press Connect, then log into OSRS.");
			}
		});
	}

	public void showMessage(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			String text = HudText.ascii(message == null ? "" : message);
			detail.setText(text.isEmpty() ? " " : wrap(text));
			fitWrapped(detail);
		});
	}

	public void showSnapshot(Snapshot snap)
	{
		if (snap == null)
		{
			snap = Snapshot.empty();
		}
		final Snapshot s = snap;
		SwingUtilities.invokeLater(() ->
		{
			linked = s.linked;
			status.setText(s.linked ? (s.loggedIn ? "Connected · in game" : "Connected · log in") : "Not connected");
			status.setForeground(s.linked ? ACCENT : MUTED);
			connectBtn.setText(s.linked ? "Disconnect" : "Connect");

			String syncLine;
			if (!s.linked)
			{
				syncLine = "Sync off";
			}
			else if (!s.loggedIn)
			{
				syncLine = "Log into OSRS to sync";
			}
			else if (s.syncing)
			{
				syncLine = "Syncing...";
			}
			else if (s.syncStatus != null && !s.syncStatus.isEmpty())
			{
				syncLine = HudText.ascii(s.syncStatus);
			}
			else
			{
				syncLine = "Sync waiting";
			}
			sync.setText(wrap(syncLine));
			fitWrapped(sync);

			if (s.showNetWorth && s.netWorth != null)
			{
				kpiInvGe.setText(PriceFormat.gpCompact(s.netWorth.invGeGp));
				kpiNet.setText(PriceFormat.gpCompact(s.netWorth.totalGp));
				if (s.netWorth.sessionArmed)
				{
					long d = s.netWorth.sessionDeltaGp;
					kpiSession.setText((d > 0 ? "+" : "") + PriceFormat.gpCompact(d));
					kpiSession.setForeground(d >= 0 ? ACCENT : new Color(0xff, 0x66, 0x66));
				}
				else
				{
					kpiSession.setText(s.netWorth.bankItemsSeen ? "..." : "bank");
					kpiSession.setForeground(MUTED);
				}
				StringBuilder meta = new StringBuilder();
				meta.append("Cash ").append(PriceFormat.gpCompact(s.inventoryGp));
				if (s.bankSeen)
				{
					meta.append(" · bank ").append(PriceFormat.gpCompact(s.bankGp));
				}
				meta.append(" · bag ").append(PriceFormat.gpCompact(s.netWorth.invItemsGp));
				meta.append(" · GE ").append(PriceFormat.gpCompact(s.netWorth.offersGp));
				if (s.cashSavedAt > 0)
				{
					meta.append(" · ").append(ago(s.cashSavedAt));
				}
				kpiMeta.setText(wrap(meta.toString()));
			}
			else
			{
				kpiInvGe.setText(PriceFormat.gpCompact(s.inventoryGp));
				kpiNet.setText(s.bankSeen ? PriceFormat.gpCompact(s.inventoryGp + s.bankGp) : "-");
				kpiSession.setText("-");
				kpiSession.setForeground(MUTED);
				kpiMeta.setText(wrap(s.bankSeen
					? ("Bank " + PriceFormat.gpCompact(s.bankGp))
					: "Open bank once for full total"));
			}
			fitWrapped(kpiMeta);

			String bankEmpty = !s.linked
				? "connect first"
				: (s.bankItemsSeen ? "no board items in bank" : "open bank to scan");
			fillBoard(liveHost, s.liveLines, s.linked ? (s.loggedIn ? "no open GE offers" : "log in") : "connect first");
			fillBoard(bankHost,
				s.loadingBoard ? Collections.singletonList(BoardLine.of(0, "loading...", "", "")) : s.bankLines,
				bankEmpty);
			fillBoard(fatHost,
				s.loadingBoard ? Collections.singletonList(BoardLine.of(0, "loading...", "", "")) : s.fatLines,
				s.linked ? "none yet" : "connect first");
			fillBoard(dipHost,
				s.loadingBoard ? Collections.singletonList(BoardLine.of(0, "loading...", "", "")) : s.dipLines,
				s.linked ? "none yet" : "connect first");
			fillBoard(flipHost,
				s.loadingFlips ? Collections.singletonList(BoardLine.of(0, "loading...", "", "")) : s.flipLines,
				"no open flips");
			fillBoard(histHost, s.historyLines, "no recent fills");

			reflowHosts();
		});
	}

	private void fillBoard(JPanel host, List<BoardLine> lines, String empty)
	{
		host.removeAll();
		int w = columnWidth();
		host.setMaximumSize(new Dimension(w, Integer.MAX_VALUE));
		if (lines == null || lines.isEmpty())
		{
			JLabel emptyLbl = plain(FontManager.getRunescapeSmallFont(), MUTED);
			emptyLbl.setText(wrap(empty));
			stretch(emptyLbl);
			fitWrapped(emptyLbl);
			host.add(emptyLbl);
		}
		else
		{
			for (BoardLine line : lines)
			{
				host.add(rowCard(line));
				host.add(Box.createVerticalStrut(4));
			}
		}
		host.revalidate();
		host.repaint();
	}

	private JPanel rowCard(BoardLine line)
	{
		int w = columnWidth();
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_LINE),
			new EmptyBorder(6, 7, 6, 7)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);

		String name = line == null ? "?" : BoardRow.name(line.row);
		String mid = line == null ? "" : BoardRow.mid(line.row);
		String total = line == null ? "" : BoardRow.total(line.row);

		JLabel nameLbl = plain(FontManager.getRunescapeBoldFont(), Color.WHITE);
		nameLbl.setText(wrap(name));
		fitWrapped(nameLbl);

		String foot = mid;
		if (!total.isEmpty())
		{
			foot = mid.isEmpty() ? total : mid + " · " + total;
		}
		JLabel footLbl = plain(FontManager.getRunescapeSmallFont(), ACCENT);
		footLbl.setForeground(MUTED);
		footLbl.setText(wrap(foot));
		fitWrapped(footLbl);
		// Highlight the profit/ETA segment in green by keeping ACCENT on a separate short line when mid exists.
		if (!mid.isEmpty() && !total.isEmpty())
		{
			footLbl.setText(wrap(mid));
			fitWrapped(footLbl);
			JLabel totalLbl = plain(FontManager.getRunescapeSmallFont(), ACCENT);
			totalLbl.setText(wrap(total));
			fitWrapped(totalLbl);
			card.add(nameLbl);
			card.add(Box.createVerticalStrut(2));
			card.add(footLbl);
			card.add(totalLbl);
		}
		else
		{
			card.add(nameLbl);
			card.add(Box.createVerticalStrut(2));
			card.add(footLbl);
		}

		int h = 0;
		for (Component c : card.getComponents())
		{
			h += Math.max(0, c.getPreferredSize().height);
		}
		h += 16;
		card.setMaximumSize(new Dimension(w, Math.max(40, h)));

		if (line != null && line.itemId > 0)
		{
			card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			card.setToolTipText("Open on Runetick");
			JPopupMenu menu = new JPopupMenu();
			JMenuItem open = new JMenuItem("Open on Runetick");
			open.addActionListener(e -> openItem(line.itemId));
			menu.add(open);
			card.setComponentPopupMenu(menu);
			MouseAdapter click = new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
					{
						openItem(line.itemId);
					}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					card.setBackground(CARD_LINE);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					card.setBackground(CARD);
				}
			};
			card.addMouseListener(click);
			for (Component c : card.getComponents())
			{
				c.addMouseListener(click);
			}
		}
		return card;
	}

	private void openItem(int itemId)
	{
		try
		{
			onOpenItem.accept(itemId);
		}
		catch (Throwable t)
		{
			showMessage("Could not open the item page");
		}
	}

	private static String ago(long at)
	{
		long mins = Math.max(0, (System.currentTimeMillis() - at) / 60_000L);
		if (mins < 1)
		{
			return "just now";
		}
		if (mins < 60)
		{
			return mins + "m ago";
		}
		long hours = mins / 60;
		if (hours < 48)
		{
			return hours + "h ago";
		}
		return (hours / 24) + "d ago";
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(ACCENT);
		g.fillRoundRect(0, 0, 16, 16, 5, 5);
		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		g.drawString("R", 4, 13);
		g.dispose();
		return image;
	}

	public static final class Snapshot
	{
		final boolean linked;
		final boolean loggedIn;
		final boolean syncing;
		final boolean loadingBoard;
		final boolean loadingFlips;
		final String syncStatus;
		final long inventoryGp;
		final long bankGp;
		final boolean bankSeen;
		final boolean bankItemsSeen;
		final long cashSavedAt;
		final boolean showNetWorth;
		final NetWorth netWorth;
		final List<BoardLine> liveLines;
		final List<BoardLine> bankLines;
		final List<BoardLine> fatLines;
		final List<BoardLine> dipLines;
		final List<BoardLine> flipLines;
		final List<BoardLine> historyLines;

		Snapshot(boolean linked, boolean loggedIn, boolean syncing, boolean loadingBoard, boolean loadingFlips,
			String syncStatus, long inventoryGp, long bankGp, boolean bankSeen, boolean bankItemsSeen, long cashSavedAt,
			boolean showNetWorth, NetWorth netWorth,
			List<BoardLine> liveLines,
			List<BoardLine> bankLines, List<BoardLine> fatLines, List<BoardLine> dipLines, List<BoardLine> flipLines,
			List<BoardLine> historyLines)
		{
			this.linked = linked;
			this.loggedIn = loggedIn;
			this.syncing = syncing;
			this.loadingBoard = loadingBoard;
			this.loadingFlips = loadingFlips;
			this.syncStatus = syncStatus == null ? "" : syncStatus;
			this.inventoryGp = inventoryGp;
			this.bankGp = bankGp;
			this.bankSeen = bankSeen;
			this.bankItemsSeen = bankItemsSeen;
			this.cashSavedAt = cashSavedAt;
			this.showNetWorth = showNetWorth;
			this.netWorth = netWorth == null ? NetWorth.empty() : netWorth;
			this.liveLines = liveLines == null ? Collections.emptyList() : liveLines;
			this.bankLines = bankLines == null ? Collections.emptyList() : bankLines;
			this.fatLines = fatLines == null ? Collections.emptyList() : fatLines;
			this.dipLines = dipLines == null ? Collections.emptyList() : dipLines;
			this.flipLines = flipLines == null ? Collections.emptyList() : flipLines;
			this.historyLines = historyLines == null ? Collections.emptyList() : historyLines;
		}

		static Snapshot empty()
		{
			return new Snapshot(false, false, false, false, false, "", 0, 0, false, false, 0,
				false, NetWorth.empty(),
				Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList());
		}
	}
}
