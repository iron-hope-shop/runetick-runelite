package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SlotProfitTest
{
	@Test
	public void aBuyIsWorthTheSellAfterTaxLessThePriceForEveryItem()
	{
		SlotProfit p = SlotProfit.forBuy(1_000, 1_000, 1_078);
		assertEquals(78_000, p.gp);
		assertEquals("+78K", p.text());
		assertEquals("if sold now", p.label);
		assertEquals("-50K", SlotProfit.forBuy(1_000, 1_100, 1_050).text());
	}

	@Test
	public void aSellIsJudgedAgainstWhatWasPaidAfterTheTwoPercentTax()
	{
		SlotProfit p = SlotProfit.forSell(10, 1_000_000, 900_000);
		assertEquals(10 * (980_000 - 900_000), p.gp);
		assertEquals("+800K", p.text());
		assertEquals("on what you paid", p.label);
	}

	@Test
	public void withoutAPriceToCompareThereIsNoEstimate()
	{
		assertNull(SlotProfit.forBuy(10, 1_000, 0));
		assertNull(SlotProfit.forSell(10, 1_000, 0));
		assertNull(SlotProfit.forBuy(0, 1_000, 1_100));
	}
}
