package com.runetick;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RuneTickOverlayTest
{
	@Test
	public void formatsGpExactlyNeverFuzzyK()
	{
		assertEquals("0", PriceFormat.gp(0));
		assertEquals("950", PriceFormat.gp(950));
		assertEquals("4,412", PriceFormat.gp(4_412));
		assertEquals("12,300", PriceFormat.gp(12_300));
		assertEquals("1,500,000", PriceFormat.gp(1_500_000));
		assertEquals("-2,000", PriceFormat.gp(-2_000));
		assertEquals("4,412 gp", PriceFormat.gpExact(4_412));
		assertEquals("4,412 -> 4,880", PriceFormat.spread(4412, 4880));
		// Compact only for huge aggregates — still more digits than "4.4k"
		assertEquals("440.5k", PriceFormat.gpCompact(440_500));
		assertEquals("1.5m", PriceFormat.gpCompact(1_500_000));
		assertEquals("1.55m", PriceFormat.gpCompact(1_550_000));
	}

	@Test
	public void abbreviatesLongItemNames()
	{
		assertEquals("Abyssal whip", PriceFormat.abbreviate("Abyssal whip", 14));
		assertEquals("Dragon plateb...", PriceFormat.abbreviate("Dragon platebody", 14));
		assertEquals("?", PriceFormat.abbreviate(null, 14));
	}
}
