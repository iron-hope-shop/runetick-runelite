package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class JournalCutoverTest
{
	@Test
	public void keepsOnlyFillsOnOrAfterCutover()
	{
		GeTrade old = trade(1_000);
		GeTrade mid = trade(2_000);
		GeTrade neu = trade(3_000);
		List<GeTrade> kept = JournalCutover.after(Arrays.asList(old, mid, neu), 2_000L, 8);
		assertEquals(2, kept.size());
		assertEquals(2_000, kept.get(0).getTime());
		assertEquals(3_000, kept.get(1).getTime());
	}

	@Test
	public void capsToLatestFills()
	{
		List<GeTrade> kept = JournalCutover.after(
			Arrays.asList(trade(1), trade(2), trade(3), trade(4)),
			null,
			2);
		assertEquals(2, kept.size());
		assertEquals(3, kept.get(0).getTime());
		assertEquals(4, kept.get(1).getTime());
	}

	@Test
	public void detectsJournalReset()
	{
		assertTrue(JournalCutover.journalReset(null, 100L));
		assertTrue(JournalCutover.journalReset(50L, 100L));
		assertFalse(JournalCutover.journalReset(100L, 100L));
		assertFalse(JournalCutover.journalReset(100L, 50L));
		assertFalse(JournalCutover.journalReset(100L, null));
	}

	private static GeTrade trade(long at)
	{
		return new com.google.gson.Gson().fromJson(
			"{\"b\":true,\"i\":1,\"q\":1,\"p\":1,\"t\":" + at + "}",
			GeTrade.class);
	}
}
