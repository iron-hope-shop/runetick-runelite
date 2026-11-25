package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NetWorthTest
{
	@Test
	public void totalsCashItemsAndOffers()
	{
		// cashGp=1m (inv+bank), invCash=400k, invItems=2m, bankItems=3m, offers=500k
		NetWorth w = new NetWorth(1_000_000, 400_000, 2_000_000, 0, 3_000_000, 500_000, true, true, 50_000, true);
		assertEquals(6_500_000, w.totalGp);
		assertEquals(2_900_000, w.invGeGp);
		assertEquals(50_000, w.sessionDeltaGp);
		assertTrue(w.sessionArmed);
	}

	@Test
	public void ignoresBankItemsUntilSeen()
	{
		NetWorth w = new NetWorth(100, 100, 200, 0, 9_999_999, 50, false, true, 0, false);
		assertEquals(350, w.totalGp);
		assertEquals(350, w.invGeGp);
		assertFalse(w.bankItemsSeen);
		assertFalse(w.sessionArmed);
	}

	@Test
	public void wornGearCountsInTheTotalButNotInTheTradingPile()
	{
		NetWorth w = new NetWorth(1_000, 1_000, 2_000, 50_000_000, 0, 300, false, true, 0, false);
		assertEquals(50_003_300, w.totalGp);
		assertEquals(3_300, w.invGeGp);
	}

	@Test
	public void leavingTheBankOutDropsBankItemsEvenWhenSeen()
	{
		NetWorth w = new NetWorth(400_000, 400_000, 2_000_000, 0, 3_000_000, 500_000, true, false, 0, true);
		assertEquals(0, w.bankItemsGp);
		assertFalse(w.bankItemsSeen);
		assertFalse(w.bankCounted);
		assertEquals(2_900_000, w.totalGp);
	}
}
