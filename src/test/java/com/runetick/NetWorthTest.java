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
		NetWorth w = new NetWorth(1_000_000, 400_000, 2_000_000, 3_000_000, 500_000, true, 50_000, true);
		assertEquals(6_500_000, w.totalGp);
		assertEquals(2_900_000, w.invGeGp);
		assertEquals(50_000, w.sessionDeltaGp);
		assertTrue(w.sessionArmed);
	}

	@Test
	public void ignoresBankItemsUntilSeen()
	{
		NetWorth w = new NetWorth(100, 100, 200, 9_999_999, 50, false, 0, false);
		assertEquals(350, w.totalGp);
		assertEquals(350, w.invGeGp);
		assertFalse(w.bankItemsSeen);
		assertFalse(w.sessionArmed);
	}
}
