package com.runetick;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class AffordTest
{
	@Test
	public void capsByCashAndLimit()
	{
		assertEquals(0, Afford.units(0, 100, 1000));
		assertEquals(0, Afford.units(50_000, 0, 1000));
		assertEquals(100, Afford.units(50_000, 500, 0));
		assertEquals(80, Afford.units(50_000, 500, 80));
		assertEquals(100, Afford.units(50_000, 500, 500));
	}
}
