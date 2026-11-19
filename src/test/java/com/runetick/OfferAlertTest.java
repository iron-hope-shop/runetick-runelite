package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OfferAlertTest
{
	@Test
	public void buyRaiseWhenMarketBuyTwoPctAboveBid()
	{
		assertEquals(OfferAlert.Kind.NONE, OfferAlert.classify(true, 1000, 1019, 900, 0, 0));
		assertEquals(OfferAlert.Kind.BUY_RAISE, OfferAlert.classify(true, 1000, 1020, 900, 0, 0));
		assertEquals(OfferAlert.Kind.BUY_RAISE, OfferAlert.classify(true, 1000, 1100, 900, 0, 0));
	}

	@Test
	public void sellLowerWhenMarketSellTwoPctBelowAsk()
	{
		assertEquals(OfferAlert.Kind.NONE, OfferAlert.classify(false, 1000, 1100, 981, 980, 800));
		assertEquals(OfferAlert.Kind.SELL_LOWER, OfferAlert.classify(false, 1000, 1100, 980, 960, 800));
	}

	@Test
	public void marginCrushWhenAfterTaxBelowFloor()
	{
		assertEquals(OfferAlert.Kind.MARGIN_CRUSH,
			OfferAlert.classify(false, 1000, 1100, 1000, 840, 800));
		assertEquals(OfferAlert.Kind.NONE,
			OfferAlert.classify(false, 1000, 1100, 1000, 900, 800));
	}

	@Test
	public void fingerprintAndMessage()
	{
		String fp = OfferAlert.fingerprint(2, 4151, OfferAlert.Kind.BUY_RAISE);
		assertEquals("2:4151:BUY_RAISE", fp);
		assertTrue(OfferAlert.message(OfferAlert.Kind.SELL_LOWER, "Abyssal whip").contains("whip"));
	}
}
