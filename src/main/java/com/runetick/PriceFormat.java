package com.runetick;

/**
 * Gold formatting. Offer / tip prices are always exact — never "4.4k".
 */
final class PriceFormat
{
	private PriceFormat()
	{
	}

	/**
	 * Exact gp with commas (e.g. {@code 4,412}). Use for buy/sell offers and GE tips.
	 */
	static String gp(long value)
	{
		long abs = Math.abs(value);
		String sign = value < 0 ? "-" : "";
		return sign + withCommas(abs);
	}

	/** Exact with a {@code gp} suffix. */
	static String gpExact(long value)
	{
		return gp(value) + " gp";
	}

	/**
	 * Compact only for huge aggregates (cash totals, gp/hour) where exact would blow the HUD.
	 * Still unambiguous: {@code 12.35m} not {@code 12m}.
	 */
	static String gpCompact(long value)
	{
		long abs = Math.abs(value);
		String sign = value < 0 ? "-" : "";
		if (abs >= 1_000_000_000L)
		{
			return sign + trim2(abs / 1_000_000_000.0) + "b";
		}
		if (abs >= 1_000_000L)
		{
			return sign + trim2(abs / 1_000_000.0) + "m";
		}
		if (abs >= 100_000L)
		{
			// e.g. 440.5k — still more digits than 4.4k
			return sign + trim1(abs / 1_000.0) + "k";
		}
		return sign + withCommas(abs);
	}

	private static String trim1(double v)
	{
		String s = String.format("%.1f", v);
		if (s.endsWith(".0"))
		{
			return s.substring(0, s.length() - 2);
		}
		return s;
	}

	private static String trim2(double v)
	{
		String s = String.format("%.2f", v);
		if (s.endsWith("0") && s.contains("."))
		{
			s = s.substring(0, s.length() - 1);
		}
		if (s.endsWith(".0"))
		{
			return s.substring(0, s.length() - 2);
		}
		if (s.endsWith("."))
		{
			return s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String withCommas(long abs)
	{
		return String.format("%,d", abs);
	}

	static String abbreviate(String name, int max)
	{
		if (name == null || name.isEmpty())
		{
			return "?";
		}
		return name.length() <= max ? name : name.substring(0, Math.max(1, max - 1)) + "...";
	}

	/** Bid->ask pair with exact prices. */
	static String spread(long bid, long ask)
	{
		return gp(bid) + " -> " + gp(ask);
	}
}
