package com.runetick;

/**
 * Compact ASCII copy for fat spreads and monthly dips (RuneLite font-safe).
 */
final class FocusCopy
{
	private FocusCopy()
	{
	}

	/** Target sell for a dip buy: ~20% after-tax profit (site evidence). */
	static long dipAsk(long buyPrice)
	{
		if (buyPrice <= 0)
		{
			return 0;
		}
		return Math.max(buyPrice + 1, Math.round(buyPrice * 1.20 / 0.98));
	}

	static String dipPct(RuneTickApi.Dip d)
	{
		if (d == null)
		{
			return "";
		}
		String pct = String.format("-%.0f%%", d.getDip() * 100.0);
		return d.isAlchFloor() ? pct + " alch" : pct;
	}

	static String dipWhy(RuneTickApi.Dip d)
	{
		return dipPct(d);
	}

	/** One-line prices: buy -> sell (usual). */
	static String dipPrices(RuneTickApi.Dip d)
	{
		if (d == null)
		{
			return "";
		}
		return PriceFormat.gp(d.getPrice())
			+ "->"
			+ PriceFormat.gp(dipAsk(d.getPrice()))
			+ " | usual "
			+ PriceFormat.gp(d.getMedian30());
	}

	static String fatWhy(RuneTickApi.FatSpread f)
	{
		if (f == null)
		{
			return "";
		}
		return PriceFormat.gp(f.getMarginPerUnit()) + "/ea | dream " + PriceFormat.gpCompact(f.getDreamGp());
	}

	static String fatPrices(RuneTickApi.FatSpread f)
	{
		if (f == null)
		{
			return "";
		}
		return PriceFormat.gp(f.getBid()) + "->" + PriceFormat.gp(f.getAsk())
			+ " x" + PriceFormat.gp(f.getDreamUnits());
	}

	static String fatMarket(RuneTickApi.FatSpread f)
	{
		if (f == null)
		{
			return "";
		}
		if (f.getMarketHint() != null && !f.getMarketHint().isEmpty())
		{
			return f.getMarketHint();
		}
		return f.getMarket() == null ? "" : f.getMarket();
	}

	static String flipLine(RuneTickApi.PlacedFlip flip)
	{
		if (flip == null)
		{
			return "";
		}
		if ("selling".equals(flip.getStatus()))
		{
			return "sell " + PriceFormat.gp(flip.getAsk())
				+ " | " + flip.getSold() + "/" + flip.getUnits();
		}
		return PriceFormat.gp(flip.getBid()) + "->" + PriceFormat.gp(flip.getAsk())
			+ " | " + flip.getBought() + "/" + flip.getUnits();
	}
}
