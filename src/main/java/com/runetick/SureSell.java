package com.runetick;

/**
 * A sell we can stand behind: the quantity already traded in the last few minutes,
 * and the price still clears tax.
 */
public final class SureSell
{
	/** How far ahead the recent 5-minute pace is allowed to speak for. */
	public static final int SOON_MINUTES = 15;
	/** Ignore a single odd print. */
	public static final int MIN_VOLUME_5M = 30;

	private SureSell()
	{
	}

	public static int soonQty(RuneTickApi.FatSpread f)
	{
		if (!tradable(f))
		{
			return 0;
		}
		int cap = f.getDreamUnits() > 0 ? f.getDreamUnits() : f.getLimit();
		int pace = f.getVolume5m() * SOON_MINUTES / 5;
		return Math.max(0, Math.min(cap, pace));
	}

	public static int minutes(RuneTickApi.FatSpread f)
	{
		int qty = soonQty(f);
		if (qty <= 0 || f.getVolume5m() <= 0)
		{
			return -1;
		}
		double perMinute = f.getVolume5m() / 5.0;
		return Math.max(1, (int) Math.ceil(qty / perMinute));
	}

	public static long soonProfit(RuneTickApi.FatSpread f)
	{
		int qty = soonQty(f);
		if (qty <= 0)
		{
			return 0;
		}
		return f.getMarginPerUnit() * (long) qty;
	}

	private static boolean tradable(RuneTickApi.FatSpread f)
	{
		if (f == null || f.getMarginPerUnit() <= 0 || f.getVolume5m() < MIN_VOLUME_5M)
		{
			return false;
		}
		return f.getDreamUnits() > 0 || f.getLimit() > 0;
	}
}
