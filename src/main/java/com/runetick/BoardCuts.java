package com.runetick;

/** Same cheap / slow / best cuts as the website fat board. */
public final class BoardCuts
{
	public static final int CHEAP_BID = 10_000;
	public static final double SLOW_CLEARS_PER_HOUR = 20;
	public static final long BEST_DREAM_GP = 5_000_000;
	/** Dips under this many traded per day count as slow. */
	public static final long SLOW_DAILY_VOLUME = 2_000;

	private BoardCuts()
	{
	}

	public static boolean fat(RuneTickApi.FatSpread f, boolean hideCheap, boolean hideSlow, boolean best)
	{
		if (f == null)
		{
			return false;
		}
		boolean cheap = f.getBid() < CHEAP_BID;
		boolean slow = f.getClearsPerHour() < SLOW_CLEARS_PER_HOUR;
		if ((hideCheap || best) && cheap)
		{
			return false;
		}
		if ((hideSlow || best) && slow)
		{
			return false;
		}
		if (best && f.getDreamGp() < BEST_DREAM_GP)
		{
			return false;
		}
		return true;
	}

	public static boolean dip(RuneTickApi.Dip d, boolean hideCheap, boolean hideSlow)
	{
		if (d == null)
		{
			return false;
		}
		if (hideCheap && d.getPrice() < CHEAP_BID)
		{
			return false;
		}
		if (hideSlow && d.getDailyVolume() < SLOW_DAILY_VOLUME)
		{
			return false;
		}
		return true;
	}
}
