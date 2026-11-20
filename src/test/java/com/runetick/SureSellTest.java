package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SureSellTest
{
	@Test
	public void onlyTheQuantityTheLastFewMinutesAlreadyTraded()
	{
		RuneTickApi.FatSpread f = spread(10_000, 80, 50);
		assertEquals(80 * SureSell.SOON_MINUTES / 5, SureSell.soonQty(f));
		assertEquals(SureSell.SOON_MINUTES, SureSell.minutes(f));
		assertTrue(SureSell.soonProfit(f) > 0);
	}

	@Test
	public void aSmallStackThatAlreadyClearsIsSooner()
	{
		RuneTickApi.FatSpread f = spread(40, 200, 10);
		assertEquals(40, SureSell.soonQty(f));
		assertTrue(SureSell.minutes(f) < SureSell.SOON_MINUTES);
	}

	@Test
	public void noMarginOrADeadBookIsNotASell()
	{
		assertEquals(0, SureSell.soonQty(spread(1000, 500, 0)));
		assertEquals(0, SureSell.soonQty(spread(1000, 5, 20)));
		assertEquals(0, SureSell.soonQty(null));
	}

	private static RuneTickApi.FatSpread spread(int dream, int volume5m, long margin)
	{
		RuneTickApi.FatSpread f = new RuneTickApi.FatSpread();
		f.setDreamUnits(dream);
		f.setLimit(dream);
		f.setVolume5m(volume5m);
		f.setMarginPerUnit(margin);
		f.setAsk(1000);
		return f;
	}
}
