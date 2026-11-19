package com.runetick;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FatStyleTest
{
	@Test
	public void busyMatchesLiquid()
	{
		RuneTickApi.FatSpread busy = fat(200, 500, 80);
		RuneTickApi.FatSpread thin = fat(2_000_000, 8, 2);
		assertTrue(FatStyle.BUSY.matches(busy));
		assertFalse(FatStyle.BUSY.matches(thin));
		assertTrue(FatStyle.PRICEY.matches(thin));
		assertFalse(FatStyle.PRICEY.matches(busy));
		assertTrue(FatStyle.ALL.matches(thin));
	}

	private static RuneTickApi.FatSpread fat(int bid, int volume5m, double clears)
	{
		RuneTickApi.FatSpread f = new RuneTickApi.FatSpread();
		f.setBid(bid);
		f.setVolume5m(volume5m);
		f.setClearsPerHour(clears);
		return f;
	}
}
