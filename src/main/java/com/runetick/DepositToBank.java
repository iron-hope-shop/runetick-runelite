package com.runetick;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * What left the bag while a deposit box was open, and how much of that the cached bank
 * does not already show. A negative live count means the bank container is not loaded.
 */
final class DepositToBank
{
	private DepositToBank()
	{
	}

	static Map<Integer, Integer> removed(Map<Integer, Integer> before, Map<Integer, Integer> after)
	{
		if (before == null || before.isEmpty())
		{
			return Collections.emptyMap();
		}
		Map<Integer, Integer> gone = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : before.entrySet())
		{
			int was = Math.max(0, e.getValue());
			int now = after == null ? 0 : Math.max(0, after.getOrDefault(e.getKey(), 0));
			if (was > now)
			{
				gone.put(e.getKey(), was - now);
			}
		}
		return gone;
	}

	static long stillMissing(long cached, long live, long removed)
	{
		if (removed <= 0 || cached < 0)
		{
			return 0;
		}
		if (live < 0)
		{
			return removed;
		}
		if (live >= cached && live - cached >= removed)
		{
			return 0;
		}
		long have = Math.max(cached, live);
		long miss = cached + removed - have;
		return miss < 0 ? 0 : miss;
	}
}
