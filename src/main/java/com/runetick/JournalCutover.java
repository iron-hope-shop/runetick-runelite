package com.runetick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Filters local GE history to what the website journal still counts. */
final class JournalCutover
{
	private JournalCutover()
	{
	}

	static List<GeTrade> after(List<GeTrade> trades, Long historyFrom, int limit)
	{
		if (trades == null || trades.isEmpty())
		{
			return Collections.emptyList();
		}
		List<GeTrade> kept = new ArrayList<>();
		for (GeTrade trade : trades)
		{
			if (trade == null)
			{
				continue;
			}
			if (historyFrom != null && trade.getTime() < historyFrom)
			{
				continue;
			}
			kept.add(trade);
		}
		int cap = Math.max(1, limit);
		if (kept.size() <= cap)
		{
			return kept;
		}
		return kept.subList(kept.size() - cap, kept.size());
	}

	/** True when the website moved the journal cutover forward (a Reset). */
	static boolean journalReset(Long previous, Long next)
	{
		if (next == null)
		{
			return false;
		}
		return previous == null || next > previous;
	}
}
