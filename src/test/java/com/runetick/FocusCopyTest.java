package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FocusCopyTest
{
	@Test
	public void eachProfitIsAfterTax()
	{
		assertEquals(-3L, FocusCopy.eachProfit(543, 551));
		assertEquals(176L, FocusCopy.eachProfit(1000, 1200));
		assertEquals(10L, FocusCopy.eachProfit(10, 20));
		assertEquals(Long.MIN_VALUE, FocusCopy.eachProfit(0, 551));
	}

	@Test
	public void dipAskTargetsTwentyPercentAfterTax()
	{
		assertEquals(1224, FocusCopy.dipAsk(1000));
		assertTrue(FocusCopy.dipAsk(650) > 650);
	}

	@Test
	public void formatsFatAndDipLines()
	{
		RuneTickApi.FatSpread fat = new RuneTickApi.FatSpread();
		fat.setBid(1000);
		fat.setAsk(1500);
		fat.setMarginPerUnit(470);
		fat.setDreamUnits(100);
		fat.setDreamGp(47_000);
		assertTrue(FocusCopy.fatPrices(fat).contains("1,000"));
		assertTrue(FocusCopy.fatWhy(fat).contains("470"));
		assertTrue(FocusCopy.fatWhy(fat).contains("full"));

		RuneTickApi.Dip dip = new RuneTickApi.Dip();
		dip.setPrice(650);
		dip.setMedian30(1559);
		dip.setDip(0.493);
		dip.setAlchFloor(true);
		assertTrue(FocusCopy.dipPct(dip).contains("49%"));
		assertTrue(FocusCopy.dipPct(dip).contains("alch"));
		assertTrue(FocusCopy.dipPrices(dip).contains("usual"));
	}
}
