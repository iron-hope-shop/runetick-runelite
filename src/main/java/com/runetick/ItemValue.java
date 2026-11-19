package com.runetick;

import java.util.Map;
import java.util.function.ToLongFunction;

/**
 * GE-price estimate for a qty map (coins/plat should already be excluded).
 */
final class ItemValue
{
	private ItemValue()
	{
	}

	static long estimate(Map<Integer, Integer> qtyById, ToLongFunction<Integer> priceOf)
	{
		if (qtyById == null || qtyById.isEmpty() || priceOf == null)
		{
			return 0;
		}
		long sum = 0;
		for (Map.Entry<Integer, Integer> e : qtyById.entrySet())
		{
			if (e == null || e.getKey() == null || e.getValue() == null)
			{
				continue;
			}
			int id = e.getKey();
			int qty = e.getValue();
			if (id <= 0 || qty <= 0)
			{
				continue;
			}
			long price = Math.max(0, priceOf.applyAsLong(id));
			if (price <= 0)
			{
				continue;
			}
			long line = price * (long) qty;
			if (line < 0)
			{
				// overflow — skip this stack rather than wrapping negative
				continue;
			}
			long next = sum + line;
			if (next < sum)
			{
				return Long.MAX_VALUE / 4;
			}
			sum = next;
		}
		return sum;
	}
}
