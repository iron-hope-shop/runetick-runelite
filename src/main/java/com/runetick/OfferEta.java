package com.runetick;

/**
 * Volume-based fill/sale ETA (wiki 5m volume → hourly pace).
 * Same spirit as site {@code clearsPerHour}: rate ≈ volume5m × 12.
 */
public final class OfferEta
{
	private OfferEta()
	{
	}

	/** Estimated units clearing per hour from 5-minute volume. */
	public static long ratePerHour(long volume5m)
	{
		if (volume5m <= 0)
		{
			return 0;
		}
		return volume5m * 12L;
	}

	/**
	 * Remaining units → estimated minutes at volume pace.
	 * Returns 0 when nothing left; Long.MAX_VALUE when volume is dead (caller shows "slow").
	 */
	public static long etaMinutes(int remaining, long volume5m)
	{
		if (remaining <= 0)
		{
			return 0;
		}
		long rate = ratePerHour(volume5m);
		if (rate <= 0)
		{
			return Long.MAX_VALUE;
		}
		long perHour = Math.max(1L, rate);
		return Math.max(1L, (remaining * 60L + perHour - 1) / perHour);
	}

	/** Compact label: {@code ~12m}, {@code ~2h}, {@code slow}, or {@code done}. */
	public static String format(int remaining, long volume5m)
	{
		if (remaining <= 0)
		{
			return "done";
		}
		long mins = etaMinutes(remaining, volume5m);
		if (mins == Long.MAX_VALUE || mins > 24L * 60L)
		{
			return "slow";
		}
		if (mins < 60)
		{
			return "~" + mins + "m";
		}
		long hours = mins / 60;
		if (hours < 48)
		{
			return "~" + hours + "h";
		}
		return "slow";
	}
}
