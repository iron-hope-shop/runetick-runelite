package com.runetick;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OfferEtaTest
{
	@Test
	public void ratePerHourScalesVolume()
	{
		assertEquals(0, OfferEta.ratePerHour(0));
		assertEquals(120, OfferEta.ratePerHour(10));
	}

	@Test
	public void etaMinutesCeilPace()
	{
		assertEquals(0, OfferEta.etaMinutes(0, 100));
		assertEquals(60, OfferEta.etaMinutes(60, 5));
		assertEquals(120, OfferEta.etaMinutes(120, 5));
		assertEquals(Long.MAX_VALUE, OfferEta.etaMinutes(10, 0));
	}

	@Test
	public void formatLabels()
	{
		assertEquals("done", OfferEta.format(0, 100));
		assertEquals("slow", OfferEta.format(10, 0));
		assertEquals("~12m", OfferEta.format(12, 5));
		assertEquals("~2h", OfferEta.format(120, 5));
		assertEquals("slow", OfferEta.format(100_000, 1));
	}
}
