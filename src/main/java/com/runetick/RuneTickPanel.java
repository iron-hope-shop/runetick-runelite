package com.runetick;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Sidebar cockpit: compact wealth tiles + card rows (width-capped — no horizontal overflow).
 * Click / right-click a row to open that item on Runetick.
 */
public class RuneTickPanel extends PluginPanel
{
	private static final Color BG = new Color(0x0b, 0x0b, 0x0c);
	private static final Color CARD = new Color(0x11, 0x11, 0x13);
	private static final Color CARD_LINE = new Color(0x27, 0x27, 0x2a);
	private static final Color INK = new Color(0xf4, 0xf4, 0xf5);
	private static final Color GAIN = new Color(0x4a, 0xde, 0x80);
	private static final Color LOSS = new Color(0xef, 0x44, 0x44);
	private static final Color MUTED = new Color(0x8b, 0x8b, 0x94);

	private final JLabel title = plain(FontManager.getRunescapeBoldFont(), Accent.color());
	private final JLabel status = plain(FontManager.getRunescapeBoldFont(), Color.WHITE);
	private final JLabel sync = plain(FontManager.getRunescapeSmallFont(), MUTED);
	private final JLabel detail = plain(FontManager.getRunescapeSmallFont(), MUTED);
	private final JLabel kpiInvGe = kpiValue();
	private final JLabel kpiNet = kpiValue();
	private final JLabel kpiSession = kpiValue();
	private final JLabel kpiMeta = plain(FontManager.getRunescapeSmallFont(), MUTED);
	private final JPanel searchHost = boardHost();
	private final JPanel itemHost = boardHost();
	private final JLabel searchLabel = section("SEARCH");
	private final JLabel searchCaption = section("Search");
	private final JPanel watchHost = boardHost();
	private final JLabel watchLabel = section("WATCHLIST");
	private final JPanel bankHost = boardHost();
	private final JPanel liveHost = boardHost();
	private final JPanel fatHost = boardHost();
	private final JPanel dipHost = boardHost();
	private final JPanel flipHost = boardHost();
	private final JPanel histHost = boardHost();
	private final JLabel offersLabel = section("Offers");
	private final JLabel bestLabel = section("Best to sell");
	private final JLabel flipsLabel = section("Open flips");
	private final JLabel fillsLabel = section("Recent fills");
	private final JPanel col = new JPanel();
	private final JButton connectBtn = new JButton();
	private final JButton siteBtn = new JButton("Open site");
	private final JButton bankBtn = new JButton("Bank view: Off");
	private Runnable onBankView = () -> { };

	private boolean linked;
	private final Runnable onConnect;
	private final Runnable onDisconnect;
	private final Runnable onOpenSite;
	private final IntConsumer onOpenItem;
	private final Consumer<String> onSearch;
	private final IntConsumer onInspect;
	private final ItemManager itemManager;

	public RuneTickPanel(
		Runnable onConnect,
		Runnable onDisconnect,
		Runnable onOpenSite,
		IntConsumer onOpenItem,
		Consumer<String> onSearch,
		IntConsumer onInspect,
		ItemManager itemManager)
	{
		this.onConnect = onConnect;
		this.onDisconnect = onDisconnect;
		this.onOpenSite = onOpenSite;
		this.onOpenItem = onOpenItem == null ? id -> { } : onOpenItem;
		this.onSearch = onSearch == null ? q -> { } : onSearch;
		this.onInspect = onInspect == null ? id -> { } : onInspect;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(BG);
		setOpaque(true);
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				capColumnWidth();
				reflowHosts();
			}
		});

		title.setText("Runetick");
		title.setForeground(Accent.color());

		flat(connectBtn);
		connectBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
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

		flat(siteBtn);
		siteBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
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
		flat(bankBtn);
		bankBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		bankBtn.setToolTipText("In the bank: mark expensive stacks, or buys you can sell for more than you paid");
		bankBtn.addActionListener(e ->
		{
			try
			{
				onBankView.run();
			}
			catch (Throwable t)
			{
				showMessage("Could not change the bank view");
			}
		});
		bankBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel wealth = wealthStrip();

		col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
		col.setOpaque(false);
		col.setAlignmentX(Component.LEFT_ALIGNMENT);
		capColumnWidth();

		status.setFont(FontManager.getRunescapeSmallFont());
		stretch(title);
		stretch(status);
		stretch(sync);
		stretch(detail);
		stretch(kpiMeta);

		JPanel head = new JPanel(new BorderLayout(8, 0));
		head.setOpaque(false);
		head.setAlignmentX(Component.LEFT_ALIGNMENT);
		head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
		head.add(title, BorderLayout.WEST);
		head.add(status, BorderLayout.EAST);

		col.add(head);
		col.add(Box.createVerticalStrut(6));
		col.add(actions);
		col.add(javax.swing.Box.createVerticalStrut(4));
		col.add(bankBtn);
		col.add(Box.createVerticalStrut(6));
		col.add(status);
		col.add(sync);
		col.add(detail);
		JTextField search = new JTextField();
		search.setFont(FontManager.getRunescapeSmallFont());
		search.setForeground(Color.WHITE);
		search.setOpaque(false);
		search.setBackground(CARD);
		search.setCaretColor(Color.WHITE);
		search.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_LINE),
			new EmptyBorder(4, 6, 4, 6)));
		search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		search.setAlignmentX(Component.LEFT_ALIGNMENT);
		search.setToolTipText("Find an item. Click a result to see buy, sell, and profit here.");
		JLabel searchHint = plain(FontManager.getRunescapeSmallFont(), MUTED);
		searchHint.setText("Search items");
		searchHint.setBorder(new EmptyBorder(0, 8, 0, 0));
		searchHint.setAlignmentX(Component.LEFT_ALIGNMENT);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			private void sync()
			{
				searchHint.setVisible(search.getText().isEmpty() && !search.isFocusOwner());
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				sync();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				sync();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				sync();
			}
		});
		search.addFocusListener(new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusGained(java.awt.event.FocusEvent e)
			{
				searchHint.setVisible(false);
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e)
			{
				searchHint.setVisible(search.getText().isEmpty());
			}
		});
		Timer searchTimer = new Timer(250, e -> onSearch.accept(search.getText()));
		searchTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchTimer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchTimer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchTimer.restart();
			}
		});

		searchLabel.setVisible(false);
		searchHost.setVisible(false);
		watchLabel.setVisible(false);
		watchHost.setVisible(false);
		itemHost.setVisible(false);

		col.add(Box.createVerticalStrut(8));
		col.add(searchCaption);
		JPanel searchBox = new JPanel();
		searchBox.setLayout(new javax.swing.OverlayLayout(searchBox));
		searchBox.setOpaque(true);
		searchBox.setBackground(CARD);
		searchBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		searchBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		search.setAlignmentX(0f);
		search.setAlignmentY(0.5f);
		searchHint.setAlignmentX(0f);
		searchHint.setAlignmentY(0.5f);
		searchHint.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				search.requestFocusInWindow();
			}
		});
		searchBox.add(search);
		searchBox.add(searchHint);
		col.add(searchBox);
		col.add(watchLabel);
		col.add(watchHost);
		col.add(Box.createVerticalStrut(4));
		col.add(searchLabel);
		col.add(searchHost);
		col.add(itemHost);
		col.add(Box.createVerticalStrut(8));
		col.add(section("WEALTH"));
		col.add(wealth);
		col.add(Box.createVerticalStrut(2));
		col.add(kpiMeta);
		col.add(Box.createVerticalStrut(10));
		col.add(bestLabel);
		col.add(fatHost);
		col.add(Box.createVerticalStrut(8));
		col.add(offersLabel);
		col.add(liveHost);
		col.add(Box.createVerticalStrut(8));
		col.add(flipsLabel);
		col.add(flipHost);
		col.add(Box.createVerticalStrut(8));
		col.add(fillsLabel);
		col.add(histHost);

		JScrollPane scroll = new JScrollPane(col);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(64);
		java.awt.event.MouseWheelListener wheel = e ->
		{
			javax.swing.JScrollBar bar = scroll.getVerticalScrollBar();
			bar.setValue(bar.getValue() + e.getWheelRotation() * 72);
			e.consume();
		};
		scroll.addMouseWheelListener(wheel);
		col.addMouseWheelListener(wheel);
		addMouseWheelListener(wheel);
		scroll.getViewport().setBackground(BG);
		scroll.setBackground(BG);
		col.setBackground(BG);
		col.setOpaque(true);
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
		JPanel strip = new JPanel(new GridLayout(1, 2, 8, 0));
		strip.setOpaque(false);
		strip.setAlignmentX(Component.LEFT_ALIGNMENT);
		strip.setMaximumSize(new Dimension(columnWidth(), 40));
		strip.setPreferredSize(new Dimension(columnWidth(), 40));
		strip.add(kpiTile("WEALTH", kpiInvGe));
		strip.add(kpiTile("SESSION", kpiSession));
		return strip;
	}

	private static JPanel kpiTile(String label, JLabel value)
	{
		JPanel tile = new JPanel();
		tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
		tile.setOpaque(false);
		tile.setBorder(new EmptyBorder(0, 0, 0, 0));
		JLabel lab = plain(FontManager.getRunescapeSmallFont(), MUTED);
		lab.setText(label);
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
		tile.add(lab);
		tile.add(value);
		return tile;
	}

	public void onBankView(Runnable action)
	{
		onBankView = action == null ? () -> { } : action;
	}

	public void setBankView(String label)
	{
		bankBtn.setText("Bank: " + label);
	}

	private static void flat(JButton button)
	{
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setBackground(CARD);
		button.setForeground(INK);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_LINE),
			new EmptyBorder(3, 8, 3, 8)));
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
		for (JPanel host : new JPanel[]{searchHost, itemHost, liveHost, bankHost, fatHost, dipHost, flipHost, histHost})
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
		JLabel l = plain(FontManager.getRunescapeSmallFont(), MUTED);
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
			title.setForeground(Accent.color());
			status.setForeground(isLinked ? INK : MUTED);
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
			title.setForeground(Accent.color());
			status.setForeground(s.linked ? INK : MUTED);
			connectBtn.setText(s.linked ? "Disconnect" : "Connect");

			String syncLine;
			if (!s.linked)
			{
				syncLine = "";
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
				syncLine = "";
			}
			sync.setText(syncLine.isEmpty() ? " " : wrap(syncLine));
			sync.setVisible(!syncLine.isEmpty());
			fitWrapped(sync);

			long spendable = s.inventoryGp + (s.bankSeen ? s.bankGp : 0);
			NetWorth worth = s.netWorth;

			if (s.showNetWorth && worth != null)
			{
				kpiInvGe.setText(PriceFormat.gpCompact(worth.totalGp));
				kpiInvGe.setForeground(INK);
				kpiNet.setText(PriceFormat.gpCompact(worth.totalGp));
				if (worth.sessionArmed)
				{
					long d = worth.sessionDeltaGp;
					kpiSession.setText((d > 0 ? "+" : "") + PriceFormat.gpCompact(d));
					kpiSession.setForeground(d >= 0 ? GAIN : LOSS);
				}
				else
				{
					kpiSession.setText("-");
					kpiSession.setForeground(MUTED);
				}
				StringBuilder meta = new StringBuilder();
				meta.append("bag ").append(PriceFormat.gpCompact(s.inventoryGp));
				if (s.bankSeen)
				{
					meta.append(" · bank ").append(PriceFormat.gpCompact(s.bankGp));
				}
				else
				{
					meta.append(" · open bank once");
				}
				meta.append(" · items ").append(PriceFormat.gpCompact(worth.invItemsGp + (worth.bankItemsSeen ? worth.bankItemsGp : 0)));
				meta.append(" · offers ").append(PriceFormat.gpCompact(worth.offersGp));
				kpiMeta.setText(wrap(meta.toString()));
			}
			else
			{
				kpiInvGe.setText(PriceFormat.gpCompact(spendable));
				kpiInvGe.setForeground(INK);
				kpiNet.setText(PriceFormat.gpCompact(spendable));
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
			fillBoard(liveHost, s.liveLines, s.linked ? (s.loggedIn ? "No offers open" : "Log in") : "Connect first");
			boolean best = s.loadingBoard || (s.fatLines != null && !s.fatLines.isEmpty());
			bestLabel.setVisible(best);
			fatHost.setVisible(best);
			boolean flips = s.loadingFlips || (s.flipLines != null && !s.flipLines.isEmpty());
			flipsLabel.setVisible(flips);
			flipHost.setVisible(flips);
			boolean fills = s.historyLines != null && !s.historyLines.isEmpty();
			fillsLabel.setVisible(fills);
			histHost.setVisible(fills);
			fillBoard(bankHost,
				s.loadingBoard ? Collections.singletonList(BoardLine.of(0, "loading...", "", "")) : s.bankLines,
				bankEmpty);
			fillBoard(fatHost,
				s.loadingBoard ? Collections.singletonList(BoardLine.of(0, "loading...", "", "")) : s.fatLines,
				s.linked ? "Nothing sells in the next 15 minutes." : "connect first");
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

	void showWatch(List<BoardLine> lines, String empty)
	{
		boolean show = (lines != null && !lines.isEmpty()) || (empty != null && !empty.isEmpty());
		watchLabel.setVisible(show);
		watchHost.setVisible(show);
		if (show)
		{
			fillBoard(watchHost, lines, empty, true);
		}
		reflowHosts();
	}

	void showSearch(List<BoardLine> lines, String empty)
	{
		boolean show = (lines != null && !lines.isEmpty()) || (empty != null && !empty.isEmpty());
		searchLabel.setVisible(show);
		searchHost.setVisible(show);
		if (!show)
		{
			clearItem();
		}
		if (show)
		{
			fillBoard(searchHost, lines, empty, true);
		}
		reflowHosts();
	}

	void showItemPending()
	{
		itemHost.removeAll();
		itemHost.setVisible(true);
		JLabel loading = plain(FontManager.getRunescapeSmallFont(), MUTED);
		loading.setText("Loading item…");
		stretch(loading);
		itemHost.add(loading);
		itemHost.revalidate();
		itemHost.repaint();
		reflowHosts();
	}

	void showItem(RuneTickApi.ItemQuote quote)
	{
		showItem(quote, null);
	}

	void showItem(RuneTickApi.ItemQuote quote, Long paidEach)
	{
		itemHost.removeAll();
		itemHost.setVisible(true);
		itemHost.add(quote == null ? note("No quote for that item.") : itemCard(quote, paidEach));
		itemHost.revalidate();
		itemHost.repaint();
		reflowHosts();
	}

	void clearItem()
	{
		itemHost.removeAll();
		itemHost.setVisible(false);
		itemHost.revalidate();
	}

	private void fillBoard(JPanel host, List<BoardLine> lines, String empty)
	{
		fillBoard(host, lines, empty, false);
	}

	private void fillBoard(JPanel host, List<BoardLine> lines, String empty, boolean inspect)
	{
		host.removeAll();
		int w = columnWidth();
		host.setMaximumSize(new Dimension(w, Integer.MAX_VALUE));
		if (lines == null || lines.isEmpty())
		{
			host.add(note(empty));
		}
		else
		{
			for (BoardLine line : lines)
			{
				host.add(rowCard(line, inspect));
				host.add(Box.createVerticalStrut(4));
			}
		}
		host.revalidate();
		host.repaint();
	}

	private JLabel note(String text)
	{
		JLabel label = plain(FontManager.getRunescapeSmallFont(), MUTED);
		label.setText(wrap(text));
		stretch(label);
		fitWrapped(label);
		return label;
	}

	private JPanel rowCard(BoardLine line, boolean inspect)
	{
		int w = columnWidth();
		JPanel card = new JPanel(new BorderLayout(6, 0));
		card.setBackground(BG);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, CARD_LINE),
			new EmptyBorder(4, 2, 4, 2)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);

		int itemId = line == null ? 0 : line.itemId;
		card.add(iconLabel(itemId), BorderLayout.WEST);

		String name = line == null ? "?" : BoardRow.name(line.row);
		String mid = line == null ? "" : BoardRow.mid(line.row);
		String total = line == null ? "" : BoardRow.total(line.row);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		JLabel nameLbl = plain(FontManager.getRunescapeBoldFont(), INK);
		nameLbl.setText(wrap(name));
		fitWrapped(nameLbl);
		text.add(nameLbl);
		if (!mid.isEmpty())
		{
			JLabel priceLbl = plain(FontManager.getRunescapeSmallFont(), INK);
			priceLbl.setText(wrap(mid));
			fitWrapped(priceLbl);
			text.add(priceLbl);
		}
		if (!total.isEmpty())
		{
			JLabel extraLbl = plain(FontManager.getRunescapeSmallFont(), moneyColor(total));
			extraLbl.setText(wrap(total));
			fitWrapped(extraLbl);
			text.add(extraLbl);
		}
		card.add(text, BorderLayout.CENTER);

		int h = Math.max(36, text.getPreferredSize().height + 12);
		card.setMaximumSize(new Dimension(w, h));
		card.setPreferredSize(new Dimension(w, h));

		if (itemId > 0)
		{
			card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			card.setToolTipText(inspect ? "Show this item here" : "Open on Runetick");
			JPopupMenu menu = new JPopupMenu();
			JMenuItem open = new JMenuItem("Open on Runetick");
			open.addActionListener(e -> openItem(itemId));
			menu.add(open);
			card.setComponentPopupMenu(menu);
			MouseAdapter click = new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1)
					{
						return;
					}
					if (inspect)
					{
						onInspect.accept(itemId);
					}
					else
					{
						openItem(itemId);
					}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					card.setBackground(CARD);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					card.setBackground(BG);
				}
			};
			wireClick(card, click);
		}
		return card;
	}

	private JPanel itemCard(RuneTickApi.ItemQuote quote, Long paidEach)
	{
		int w = columnWidth();
		JPanel card = new JPanel(new BorderLayout(6, 0));
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_LINE),
			new EmptyBorder(6, 6, 6, 6)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.add(iconLabel(quote.getId()), BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		String name = quote.getName() == null || quote.getName().isEmpty() ? "Item" : quote.getName();
		JLabel nameLbl = plain(FontManager.getRunescapeBoldFont(), INK);
		nameLbl.setText(wrap(name));
		fitWrapped(nameLbl);
		text.add(nameLbl);
		if (paidEach != null && paidEach > 0)
		{
			text.add(line("Paid", PriceFormat.gp(paidEach), INK));
		}
		else
		{
			text.add(line("Mkt buy", PriceFormat.gp(quote.getBuy()), INK));
		}
		text.add(line("Mkt sell", PriceFormat.gp(quote.getSell()), INK));
		long each = FocusCopy.eachProfit(paidEach != null && paidEach > 0 ? paidEach : quote.getBuy(), quote.getSell());
		if (each != Long.MIN_VALUE)
		{
			String signed = (each > 0 ? "+" : "") + PriceFormat.gp(each);
			text.add(line(paidEach != null && paidEach > 0 ? "vs paid" : "Each", signed, each >= 0 ? GAIN : LOSS));
		}
		if (quote.getHighalch() != null && quote.getHighalch() > 0)
		{
			Long edge = quote.getAlchBeatsSell();
			if (edge != null && edge > 0)
			{
				text.add(line("Alch", "+" + PriceFormat.gp(edge) + " vs sell", GAIN));
			}
			else
			{
				text.add(line("Alch", PriceFormat.gp(quote.getHighalch()) + " · sell is better", MUTED));
			}
		}
		if (each != Long.MIN_VALUE)
		{
			text.add(line("×10", signedQty(each, 10), each >= 0 ? GAIN : LOSS));
			text.add(line("×100", signedQty(each, 100), each >= 0 ? GAIN : LOSS));
			if (quote.getLimit() > 0)
			{
				text.add(line("Full limit", signedQty(each, quote.getLimit()), each >= 0 ? GAIN : LOSS));
			}
		}
		if (quote.getLimit() > 0)
		{
			text.add(line("Limit", PriceFormat.gp(quote.getLimit()), INK));
		}
		if (quote.getVolume() > 0)
		{
			text.add(line("Volume", PriceFormat.gp(quote.getVolume()), INK));
		}
		JButton open = new JButton("Open on site");
		flat(open);
		open.setAlignmentX(Component.LEFT_ALIGNMENT);
		open.setMaximumSize(new Dimension(w, 24));
		open.addActionListener(e -> openItem(quote.getId()));
		text.add(Box.createVerticalStrut(4));
		text.add(open);
		card.add(text, BorderLayout.CENTER);

		int h = Math.max(88, text.getPreferredSize().height + 16);
		card.setMaximumSize(new Dimension(w, h));
		card.setPreferredSize(new Dimension(w, h));
		return card;
	}

	private static String signedQty(long each, int qty)
	{
		long total = each * qty;
		return (total > 0 ? "+" : "") + PriceFormat.gp(total);
	}

	private JLabel line(String label, String value, Color valueColor)
	{
		JLabel row = plain(FontManager.getRunescapeSmallFont(), valueColor);
		row.setText(wrap(label + "  " + value));
		fitWrapped(row);
		return row;
	}

	private JLabel iconLabel(int itemId)
	{
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setMinimumSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		if (itemManager == null || itemId <= 0)
		{
			return icon;
		}
		try
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			if (image != null)
			{
				image.addTo(icon);
			}
		}
		catch (Throwable ignored)
		{
			// A missing sprite should not blank the row.
		}
		return icon;
	}

	private static Color moneyColor(String text)
	{
		if (text == null || text.isEmpty())
		{
			return MUTED;
		}
		char c = text.charAt(0);
		if (c == '+')
		{
			return GAIN;
		}
		if (c == '-' || c == '\u2212')
		{
			return LOSS;
		}
		return MUTED;
	}

	private static void wireClick(Component component, MouseAdapter click)
	{
		component.addMouseListener(click);
		if (component instanceof java.awt.Container)
		{
			for (Component child : ((java.awt.Container) component).getComponents())
			{
				wireClick(child, click);
			}
		}
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
		try (java.io.InputStream in = RuneTickPanel.class.getResourceAsStream("icon-16.png"))
		{
			if (in != null)
			{
				BufferedImage image = javax.imageio.ImageIO.read(in);
				if (image != null)
				{
					return image;
				}
			}
		}
		catch (java.io.IOException e)
		{
			// Fall through to a plain tile if the icon file is missing.
		}
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Accent.color());
		g.fillRoundRect(0, 0, 16, 16, 4, 4);
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
