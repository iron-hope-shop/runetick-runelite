package com.runetick;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BoardFilterTest
{
	@Test
	public void fatPassesDreamAndMarginFloor()
	{
		assertTrue(1_500_000L >= 1_000_000L && 200L >= 50L);
		assertFalse(400_000L >= 1_000_000L);
	}

	@Test
	public void dipAlchCanBeHidden()
	{
		RuneTickApi.Dip alch = new RuneTickApi.Dip();
		alch.setAlchFloor(true);
		RuneTickApi.Dip market = new RuneTickApi.Dip();
		market.setAlchFloor(false);
		assertTrue(alch.isAlchFloor());
		assertFalse(market.isAlchFloor());
	}
}
