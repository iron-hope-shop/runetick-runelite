package com.runetick;

/**
 * Cash + GE-priced items + open GE offers. Bank item value is 0 until the bank has been opened once.
 * Session delta is only meaningful once {@link #sessionArmed} (inv + bank scanned).
 *
 * <p>{@link #invGeGp} is bag cash + bag items + open GE offers (no bank) — the live trading pile.
 */
final class NetWorth
{
	final long cashGp;
	final long inventoryCashGp;
	final long invItemsGp;
	final long bankItemsGp;
	final long offersGp;
	final boolean bankItemsSeen;
	/** Inv cash + inv items + GE offers (excludes bank). */
	final long invGeGp;
	final long totalGp;
	final long sessionDeltaGp;
	final boolean sessionArmed;

	NetWorth(long cashGp, long inventoryCashGp, long invItemsGp, long bankItemsGp, long offersGp,
		boolean bankItemsSeen, long sessionDeltaGp, boolean sessionArmed)
	{
		this.cashGp = Math.max(0, cashGp);
		this.inventoryCashGp = Math.max(0, inventoryCashGp);
		this.invItemsGp = Math.max(0, invItemsGp);
		this.bankItemsGp = Math.max(0, bankItemsGp);
		this.offersGp = Math.max(0, offersGp);
		this.bankItemsSeen = bankItemsSeen;
		long invGe = this.inventoryCashGp + this.invItemsGp + this.offersGp;
		this.invGeGp = invGe < 0 ? Long.MAX_VALUE / 4 : invGe;
		long items = this.invItemsGp + (bankItemsSeen ? this.bankItemsGp : 0);
		long total = this.cashGp + items + this.offersGp;
		this.totalGp = total < 0 ? Long.MAX_VALUE / 4 : total;
		this.sessionDeltaGp = sessionDeltaGp;
		this.sessionArmed = sessionArmed;
	}

	static NetWorth empty()
	{
		return new NetWorth(0, 0, 0, 0, 0, false, 0, false);
	}
}
