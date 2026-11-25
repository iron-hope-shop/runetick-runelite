package com.runetick;

/**
 * Estimated profit for one Grand Exchange slot, after tax, on the whole offer.
 * A buy is valued at the current sell price after tax. A sell is valued against what
 * Grand Exchange history says was paid for it; without that history there is no estimate.
 */
final class SlotProfit
{
	final long gp;
	final String label;

	private SlotProfit(long gp, String label)
	{
		this.gp = gp;
		this.label = label;
	}

	static SlotProfit forBuy(int total, long price, long afterTaxSell)
	{
		if (total <= 0 || price <= 0 || afterTaxSell <= 0)
		{
			return null;
		}
		long gp = clamp((afterTaxSell - price) * (long) total);
		return new SlotProfit(gp, "if sold now");
	}

	static SlotProfit forSell(int total, long price, long paidEach)
	{
		if (total <= 0 || price <= 0 || paidEach <= 0)
		{
			return null;
		}
		long gp = clamp((BankView.afterTax(price) - paidEach) * (long) total);
		return new SlotProfit(gp, "on what you paid");
	}

	/** "+1.2M" or "-340K". */
	String text()
	{
		return (gp > 0 ? "+" : gp < 0 ? "-" : "") + BankView.compact(Math.abs(gp));
	}

	private static long clamp(long v)
	{
		long cap = Long.MAX_VALUE / 4;
		return Math.max(-cap, Math.min(cap, v));
	}
}
