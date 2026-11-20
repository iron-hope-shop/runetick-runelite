package com.runetick;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BoardCutsTest
{
	@Test
	public void bestRequiresTicketSpeedAndDream()
	{
		RuneTickApi.FatSpread cheap = fat(500, 100, 2_000_000);
		RuneTickApi.FatSpread slow = fat(80_000, 5, 9_000_000);
		RuneTickApi.FatSpread small = fat(80_000, 40, 1_000_000);
		RuneTickApi.FatSpread best = fat(80_000, 40, 8_000_000);
		assertFalse(BoardCuts.fat(cheap, true, false, false));
		assertFalse(BoardCuts.fat(slow, false, true, false));
		assertFalse(BoardCuts.fat(small, false, false, true));
		assertTrue(BoardCuts.fat(best, true, true, true));
	}

	private static RuneTickApi.FatSpread fat(int bid, double clears, long dream)
	{
		RuneTickApi.FatSpread f = new RuneTickApi.FatSpread();
		f.setBid(bid);
		f.setClearsPerHour(clears);
		f.setDreamGp(dream);
		return f;
	}
}
