package com.runetick;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToLongFunction;

/**
 * What to show on items in the bank: stacks worth a lot, or items bought on the
 * Grand Exchange and still held, with how the sell price compares to what was paid.
 */
public final class BankView
{
	public enum Mode
	{
		OFF("Off"),
		VALUE("High value"),
		SELL("To sell");

		private final String label;

		Mode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}

		public Mode next()
		{
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public static final class Mark
	{
		public final String label;
		/** 1 sells for more than was paid, -1 for less, 0 no judgement. */
		public final int tone;

		Mark(String label, int tone)
		{
			this.label = label;
			this.tone = tone;
		}
	}

	static final long TAX_FREE_UNDER = 50;
	static final long TAX_CAP = 5_000_000;

	private BankView()
	{
	}

	/** What one item keeps after the Grand Exchange's 2% tax. */
	static long afterTax(long price)
	{
		if (price < TAX_FREE_UNDER)
		{
			return Math.max(0, price);
		}
		return price - Math.min(TAX_CAP, price / 50);
	}

	static String compact(long gp)
	{
		long abs = Math.abs(gp);
		if (abs >= 1_000_000_000L)
		{
			return oneDecimal(abs / 1_000_000_000.0) + "B";
		}
		if (abs >= 1_000_000L)
		{
			return oneDecimal(abs / 1_000_000.0) + "M";
		}
		if (abs >= 1_000L)
		{
			return (abs / 1_000L) + "K";
		}
		return Long.toString(abs);
	}

	private static String oneDecimal(double v)
	{
		long tenths = (long) Math.floor(v * 10);
		return tenths % 10 == 0 ? Long.toString(tenths / 10) : (tenths / 10) + "." + (tenths % 10);
	}

	/** Stacks worth at least {@code minStackGp}, labelled with the stack's value. */
	static Map<Integer, Mark> valueMarks(Map<Integer, Integer> qtyById, ToLongFunction<Integer> priceOf, long minStackGp)
	{
		if (qtyById == null || qtyById.isEmpty() || priceOf == null)
		{
			return Collections.emptyMap();
		}
		Map<Integer, Mark> out = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : qtyById.entrySet())
		{
			if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0)
			{
				continue;
			}
			long price = Math.max(0, priceOf.applyAsLong(e.getKey()));
			long stack = price * (long) e.getValue();
			if (price > 0 && stack >= Math.max(1, minStackGp))
			{
				out.put(e.getKey(), new Mark(compact(stack), 0));
			}
		}
		return out;
	}

	/**
	 * Held items with a known buy price: the percent the sell keeps, after tax, over what was paid.
	 * {@code costOf} returns 0 when Grand Exchange history has no unsold buy for the item.
	 */
	static Map<Integer, Mark> profitMarks(Collection<Integer> ids, ToLongFunction<Integer> priceOf, ToLongFunction<Integer> costOf)
	{
		if (ids == null || ids.isEmpty() || priceOf == null || costOf == null)
		{
			return Collections.emptyMap();
		}
		Map<Integer, Mark> out = new HashMap<>();
		for (Integer id : ids)
		{
			if (id == null)
			{
				continue;
			}
			long cost = costOf.applyAsLong(id);
			long price = priceOf.applyAsLong(id);
			if (cost <= 0 || price <= 0)
			{
				continue;
			}
			long keep = afterTax(price) - cost;
			long pct = Math.round(keep * 100.0 / cost);
			String label = (pct > 0 ? "+" : "") + pct + "%";
			out.put(id, new Mark(label, keep > 0 ? 1 : keep < 0 ? -1 : 0));
		}
		return out;
	}
}
