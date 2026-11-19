package com.runetick;

/**
 * Human-readable day-trade lines from mean-reversion signals (ASCII for RuneLite font).
 */
final class DayTradeCopy
{
	private DayTradeCopy()
	{
	}

	static String action(RuneTickApi.Signal s)
	{
		if (s == null)
		{
			return "Watch";
		}
		return "cheap".equals(s.getSide()) ? "Buy dip" : "Sell spike";
	}

	static String reason(RuneTickApi.Signal s)
	{
		if (s == null)
		{
			return "";
		}
		double z = s.getZScore24h();
		String strength;
		double abs = Math.abs(z);
		if (abs >= 2.5)
		{
			strength = "strong";
		}
		else if (abs >= 1.5)
		{
			strength = "solid";
		}
		else
		{
			strength = "mild";
		}
		if ("cheap".equals(s.getSide()))
		{
			return strength + " dip | z " + formatZ(z) + " vs 24h";
		}
		return strength + " spike | z " + formatZ(z) + " vs 24h";
	}

	static String prices(RuneTickApi.Signal s)
	{
		if (s == null)
		{
			return "";
		}
		if ("cheap".equals(s.getSide()))
		{
			return "buy <= " + PriceFormat.gp(s.getBuy()) + " -> sell " + PriceFormat.gp(s.getSell());
		}
		return "sell >= " + PriceFormat.gp(s.getSell()) + " (bid " + PriceFormat.gp(s.getBuy()) + ")";
	}

	static String hint(RuneTickApi.Signal s)
	{
		if (s == null)
		{
			return "";
		}
		if (s.getHint() != null && !s.getHint().isEmpty())
		{
			return s.getHint();
		}
		if (s.getMarket() != null && !s.getMarket().isEmpty())
		{
			return s.getMarket();
		}
		return "";
	}

	private static String formatZ(double z)
	{
		return String.format("%+.1f", z);
	}
}
