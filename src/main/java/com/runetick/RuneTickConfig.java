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
	String THIRD_PARTY = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	@ConfigItem(
		keyName = "syncPlaced",
		name = "Update placed flips",
		description = "When a Grand Exchange offer matches a flip you placed in Runetick, update how many have bought and sold",
		warning = THIRD_PARTY,
		position = 1
	)
	default boolean syncPlaced()
	{
		return false;
	}

	@ConfigItem(
		keyName = "logTrades",
		name = "Log other trades",
		description = "Log finished Grand Exchange buys and sells that are not one of your placed flips to your Runetick trade log",
		warning = THIRD_PARTY,
		position = 2
	)
	default boolean logTrades()
	{
		return false;
	}

	@ConfigItem(
		keyName = "syncHistory",
		name = "Sync GE history & cash",
		description = "Upload GE history, live GE slots, cash, and net worth to the Runetick website",
		warning = THIRD_PARTY,
		position = 3
	)
	default boolean syncHistory()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showStatusHud",
		name = "Tiny status HUD",
		description = "Optional 2-line on-screen chip. Fat/dips/flips/cash live in the sidebar panel; GE tips still show when you open an item",
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
		warning = THIRD_PARTY,
		position = 5
	)
	default boolean showGeTips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showItemHighlights",
		name = "Highlight flip items",
		description = "Tag recommended and open-flip items in inventory, bank, and GE inventory",
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
		description = "When you open the bank, match holdings against fat/dips/flips and show BANK HITS in the sidebar",
		position = 8
	)
	default boolean scanBankItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFatAndDips",
		name = "Fat spreads & dips",
		description = "Show wide after-tax gaps and monthly dips from Runetick",
		warning = THIRD_PARTY,
		position = 9
	)
	default boolean showFatAndDips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fatStyle",
		name = "Fat style",
		description = "ALL = every fat row. BUSY = high volume / liquid. PRICEY = high ticket with thinner volume",
		position = 10
	)
	default FatStyle fatStyle()
	{
		return FatStyle.ALL;
	}

	@ConfigItem(
		keyName = "minDreamProfitGp",
		name = "Min fat profit",
		description = "Hide fat spreads whose dream-stack profit is below this",
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
		description = "Hide fat/dip rows whose after-tax margin per item is below this",
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
		description = "Flash / tray notify on flip updates and logged trades (works in fullscreen)",
		position = 17
	)
	default boolean desktopNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "offerEta",
		name = "Offer fill ETA",
		description = "Estimate sale/fill time from wiki 5m volume on live GE cards and GE tip",
		warning = THIRD_PARTY,
		position = 18
	)
	default boolean offerEta()
	{
		return false;
	}

	@ConfigItem(
		keyName = "offerPriceAlerts",
		name = "Offer price alerts",
		description = "Desktop notify when market moves ~2% against an open GE offer (deduped per slot)",
		warning = THIRD_PARTY,
		position = 19
	)
	default boolean offerPriceAlerts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "siteUrl",
		name = "Runetick address",
		description = "Where Runetick lives. Only change this to test against a local copy, e.g. http://localhost:3002",
		position = 20
	)
	default String siteUrl()
	{
		return DEFAULT_SITE_URL;
	}
}
