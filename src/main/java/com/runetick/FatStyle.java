package com.runetick;

/**
 * Fat board slice — mirrors the site's High volume / Expensive filters.
 */
public enum FatStyle
{
	ALL,
	BUSY,
	PRICEY;

	/** ~100 trades / 5m or ~40 clears/h. */
	public static final int BUSY_VOLUME_5M = 100;
	public static final double BUSY_CLEARS_PER_HOUR = 40;
	/** High unit price for the thin/expensive lane. */
	public static final int PRICEY_BID = 50_000;

	public boolean matches(RuneTickApi.FatSpread f)
	{
		if (f == null)
		{
			return false;
		}
		if (this == ALL)
		{
			return true;
		}
		boolean busy = f.getVolume5m() >= BUSY_VOLUME_5M || f.getClearsPerHour() >= BUSY_CLEARS_PER_HOUR;
		if (this == BUSY)
		{
			return busy;
		}
		return f.getBid() >= PRICEY_BID && !busy;
	}
}
