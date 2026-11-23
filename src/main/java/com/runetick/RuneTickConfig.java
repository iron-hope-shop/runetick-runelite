package com.runetick;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(RuneTickConfig.GROUP)
public interface RuneTickConfig extends Config
{
	String GROUP = "runetick";
	String DEFAULT_SITE_URL = "https://runetick.ironhopeshop.com";
	@ConfigItem(
		keyName = "syncPlaced",
		name = "Update placed flips",
		description = "When a Grand Exchange offer matches a flip you placed in Runetick, update how many have bought and sold",
		position = 1
	)
	default boolean syncPlaced()
	{
		return true;
	}

	@ConfigItem(
		keyName = "logTrades",
		name = "Log other trades",
		description = "Log finished Grand Exchange buys and sells that are not one of your placed flips to your Runetick trade log",
		position = 2
	)
	default boolean logTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncHistory",
		name = "Sync GE history & cash",
		description = "Upload GE history, live GE slots, cash, and net worth to the Runetick website",
		position = 3
	)
	default boolean syncHistory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStatusHud",
		name = "Tiny status HUD",
		description = "Optional 2-line on-screen chip. Margins, flips, and cash stay in the sidebar; GE tips still show when you open an item",
		position = 4
	)
	default boolean showStatusHud()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showGeTips",
		name = "GE offer tips",
		description = "When the Grand Exchange is open on an item, show recommended buy/sell vs market averages",
		position = 5
	)
	default boolean showGeTips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showItemHighlights",
		name = "Tag my items",
		description = "On items in your open flips, show the price to buy or sell at. On items you bought on the Grand Exchange and still hold, show how much the sell is up or down on what you paid",
		position = 6
	)
	default boolean showItemHighlights()
	{
		return true;
	}

	@ConfigItem(
		keyName = "openItemOnSite",
		name = "Right-click -> site",
		description = "Add a Runetick option on item right-click (inventory/bank/GE) and open board rows on click",
		position = 7
	)
	default boolean openItemOnSite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "scanBankItems",
		name = "Scan bank for board hits",
		description = "When you open the bank, match holdings against the margins and show BANK HITS in the sidebar",
		position = 8
	)
	default boolean scanBankItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFatAndDips",
		name = "Margins",
		description = "Show after-tax margins and items sitting below their usual price",
		position = 9
	)
	default boolean showFatAndDips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fatStyle",
		name = "Board",
		description = "ALL = every row. BUSY = high volume. PRICEY = high ticket with thinner volume",
		position = 10
	)
	default FatStyle fatStyle()
	{
		return FatStyle.ALL;
	}

	@ConfigItem(
		keyName = "hideCheap",
		name = "Hide cheap items",
		description = "Hide gaps and dips cheaper than 10,000 gp each",
		position = 101
	)
	default boolean hideCheap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideSlow",
		name = "Hide slow items",
		description = "Hide gaps under 20 clears an hour, and dips under 2,000 traded a day",
		position = 102
	)
	default boolean hideSlow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bestOnly",
		name = "Best only",
		description = "Only items that are not cheap, not slow, and at least 5m full-limit profit",
		position = 103
	)
	default boolean bestOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "minDreamProfitGp",
		name = "Min full-limit profit",
		description = "Hide rows whose full-limit profit is below this",
		position = 11
	)
	@Range(min = 0, max = 100_000_000)
	@Units("gp")
	default int minDreamProfitGp()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "minMarginPerUnit",
		name = "Min margin / item",
		description = "Hide rows whose after-tax margin per item is below this",
		position = 12
	)
	@Range(min = 0, max = 1_000_000)
	@Units("gp")
	default int minMarginPerUnit()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "hideAlchDips",
		name = "Hide alch dips",
		description = "Hide dips that are only sitting on high-alch floor (if you are not alching)",
		position = 13
	)
	default boolean hideAlchDips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showCash",
		name = "Show cash",
		description = "Show inventory gold and last-known bank gold in the sidebar (and tiny HUD if enabled). Bank is remembered across restarts",
		position = 14
	)
	default boolean showCash()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNetWorth",
		name = "Show net worth",
		description = "Estimate net worth from cash + GE prices of inventory/bank items + open GE offers (open bank once for full total). Shows session delta",
		position = 15
	)
	default boolean showNetWorth()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGeHistory",
		name = "Show GE history",
		description = "Show recent completed Grand Exchange fills in the sidebar panel",
		position = 16
	)
	default boolean showGeHistory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "desktopNotify",
		name = "Desktop notifications",
		description = "Master switch for tray notices. The switches below choose which events still speak",
		position = 17
	)
	default boolean desktopNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyFlipUpdates",
		name = "Notify when a flip updates",
		description = "Tray notice when a Grand Exchange offer matches a flip you saved on the site",
		position = 18
	)
	default boolean notifyFlipUpdates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyLoggedTrades",
		name = "Notify when a fill is logged",
		description = "Tray notice when a completed buy or sell is written to the journal",
		position = 19
	)
	default boolean notifyLoggedTrades()
	{
		return true;
	}

	@ConfigItem(
		keyName = "offerEta",
		name = "Offer fill ETA",
		description = "Time left on open offers (desk pace, queue position, round-fill odds) on live cards and GE tip",
		position = 20
	)
	default boolean offerEta()
	{
		return true;
	}

	@ConfigItem(
		keyName = "offerPriceAlerts",
		name = "Offer price alerts",
		description = "Desktop notice when the market moves against an open Grand Exchange offer. The next settings set how far, and which cases",
		position = 21
	)
	default boolean offerPriceAlerts()
	{
		return true;
	}

	@Range(min = 1, max = 25)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "offerAlertPercent",
		name = "Offer alert at",
		description = "How far the live price must move against your offer before a notice. 2 means two percent",
		position = 22
	)
	default int offerAlertPercent()
	{
		return 2;
	}

	@Range(min = 0, max = 1_000_000)
	@ConfigItem(
		keyName = "offerMarginFloor",
		name = "Margin floor (gp)",
		description = "On a sell, notice when the after-tax profit per item falls under this",
		position = 23
	)
	default int offerMarginFloor()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "alertBuyRaise",
		name = "Buy price rose",
		description = "Notice when the market buy climbs above your bid",
		position = 24
	)
	default boolean alertBuyRaise()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertSellLower",
		name = "Sell price fell",
		description = "Notice when the market sell drops under your ask",
		position = 25
	)
	default boolean alertSellLower()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertMarginCrush",
		name = "Profit crushed",
		description = "Notice when a sell no longer clears the margin floor",
		position = 26
	)
	default boolean alertMarginCrush()
	{
		return true;
	}

	@ConfigItem(
		keyName = "siteUrl",
		name = "Runetick address",
		description = "Where Runetick lives. Only change this to test against a local copy, e.g. http://localhost:3002",
		position = 27
	)
	default String siteUrl()
	{
		return DEFAULT_SITE_URL;
	}

	@ConfigItem(
		keyName = "bankView",
		name = "Bank view",
		description = "High value marks stacks worth at least the amount below and dims the rest. To sell marks items you bought on the Grand Exchange and still hold: green sells for more than you paid, red for less",
		position = 30
	)
	default BankView.Mode bankView()
	{
		return BankView.Mode.OFF;
	}

	@ConfigItem(
		keyName = "bankValueMin",
		name = "High value from (gp)",
		description = "The smallest stack value the High value bank view marks",
		position = 31
	)
	default int bankValueMin()
	{
		return 1_000_000;
	}
}
