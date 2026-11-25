package com.runetick;

/**
 * Cash + items priced at the Grand Exchange + what open offers hold. Items are the bag, worn gear,
 * and the last-seen bank. The bank is left out entirely when {@link #bankCounted} is false.
 * Session delta is only meaningful once {@link #sessionArmed}.
 *
 * <p>{@link #invGeGp} is bag cash + bag items + open GE offers (no bank, no worn gear) — the live trading pile.
 */
final class NetWorth
{
	final long cashGp;
	final long inventoryCashGp;
	final long invItemsGp;
	final long wornGp;
	final long bankItemsGp;
	final long offersGp;
	final boolean bankItemsSeen;
	final boolean bankCounted;
	/** Inv cash + inv items + GE offers (excludes bank and worn gear). */
	final long invGeGp;
	final long totalGp;
	final long sessionDeltaGp;
	final boolean sessionArmed;

	NetWorth(long cashGp, long inventoryCashGp, long invItemsGp, long wornGp, long bankItemsGp, long offersGp,
		boolean bankItemsSeen, boolean bankCounted, long sessionDeltaGp, boolean sessionArmed)
	{
		this.cashGp = Math.max(0, cashGp);
		this.inventoryCashGp = Math.max(0, inventoryCashGp);
		this.invItemsGp = Math.max(0, invItemsGp);
		this.wornGp = Math.max(0, wornGp);
		this.bankCounted = bankCounted;
		this.bankItemsGp = bankCounted ? Math.max(0, bankItemsGp) : 0;
		this.offersGp = Math.max(0, offersGp);
		this.bankItemsSeen = bankCounted && bankItemsSeen;
		this.invGeGp = sum(this.inventoryCashGp, this.invItemsGp, this.offersGp);
		this.totalGp = sum(this.cashGp, this.invItemsGp, this.wornGp, this.bankItemsSeen ? this.bankItemsGp : 0, this.offersGp);
		this.sessionDeltaGp = sessionDeltaGp;
		this.sessionArmed = sessionArmed;
	}

	private static long sum(long... parts)
	{
		long total = 0;
		for (long p : parts)
		{
			total += p;
			if (total < 0)
			{
				return Long.MAX_VALUE / 4;
			}
		}
		return total;
	}

	static NetWorth empty()
	{
		return new NetWorth(0, 0, 0, 0, 0, 0, false, true, 0, false);
	}
}
