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
	public void deskPaceCapsByLimit()
	{
		assertEquals(6.0, OfferEta.deskUnitsPerHour(5, 10_000), 0.01);
	}

	@Test
	public void etaMinutesCeilPace()
	{
		assertEquals(0, OfferEta.etaMinutes(0, 100));
		OfferEta.Input in = new OfferEta.Input();
		in.remaining = 120;
		in.volume5m = 10;
		in.limit = 1000;
		in.buy = false;
		in.offerPrice = 100;
		in.marketBuy = 100;
		in.marketSell = 105;
		assertEquals(600, OfferEta.etaMinutes(in));
		assertEquals(Long.MAX_VALUE, OfferEta.etaMinutes(10, 0));
	}

	@Test
	public void formatLabels()
	{
		assertEquals("done", OfferEta.format(0, 100));
		assertEquals("slow", OfferEta.format(10, 0));
		OfferEta.Input desk = new OfferEta.Input();
		desk.remaining = 12;
		desk.volume5m = 5;
		desk.limit = 10_000;
		assertEquals("~2h", OfferEta.format(desk));
		OfferEta.Input huge = new OfferEta.Input();
		huge.remaining = 100_000;
		huge.volume5m = 1;
		huge.limit = 100;
		assertEquals("slow", OfferEta.format(huge));
	}

	@Test
	public void paceLabelWhenOffQueue()
	{
		assertEquals("not filling", OfferEta.paceLabel(OfferAlert.Kind.BUY_RAISE, "~2h"));
		assertEquals("~2h", OfferEta.paceLabel(OfferAlert.Kind.NONE, "~2h"));
	}
}