package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MenuCopyTest
{
	private static RuneTickApi.ItemQuote quote(long buy, long sell)
	{
		RuneTickApi.ItemQuote q = new RuneTickApi.ItemQuote();
		q.setId(1);
		q.setName("Raw summer pie");
		q.setBuy(buy);
		q.setSell(sell);
		q.setLimit(13000);
		return q;
	}

	@Test
	public void priceCheckGivesProfitAfterTaxAndOdds()
	{
		RuneTickApi.ItemQuote q = quote(3100, 3349);
		RuneTickApi.Analytics a = new RuneTickApi.Analytics();
		a.setFillProb(0.42);
		a.setZScore24h(-1.5);
		q.setAnalytics(a);
		assertEquals("Raw summer pie: buy 3,100 · sell 3,349 · +183 each after tax · fills 4 in 10 · below usual · limit 13,000",
			MenuCopy.priceCheck("Raw summer pie", q));
		assertEquals("Raw summer pie: no recent trades.", MenuCopy.priceCheck("Raw summer pie", null));
	}

	@Test
	public void sellValueComparesAlchAndWarnsOnBigStacks()
	{
		RuneTickApi.ItemQuote q = quote(1000, 1060);
		q.setHighalch(1200);
		q.setLimit(100);
		String line = MenuCopy.sellValue("Raw bluefin", 7681, q);
		assertTrue(line, line.startsWith("7,681 × Raw bluefin: sells for about 7.9M after tax (1,039 each). High alch 9.2M, better than selling"));
		assertTrue(line, line.endsWith("push the price down."));
		assertEquals("You have no Raw bluefin in your bag or bank.", MenuCopy.sellValue("Raw bluefin", 0, q));
	}

	@Test
	public void slotCallExplainsEachMark()
	{
		RuneTickApi.ItemQuote q = quote(9265, 9662);
		String raise = MenuCopy.slotCall("Tooth half of key", true, 9000, 499, 11000, q, OfferAlert.Kind.BUY_RAISE);
		assertTrue(raise, raise.startsWith("± Reprice Tooth half of key (499/11,000 done): people are now paying 9,265, 2.9% over your 9,000."));
		String hold = MenuCopy.slotCall("Tooth half of key", true, 9202, 0, 0, q, OfferAlert.Kind.NONE);
		assertEquals("✓ Hold Tooth half of key: your 9,202 is in line with the market (9,265). Selling at today's price makes +267 each after tax.", hold);
		String lower = MenuCopy.slotCall("Tooth half of key", false, 10000, 0, 458, q, OfferAlert.Kind.SELL_LOWER);
		assertTrue(lower, lower.contains("under your 10,000. Lower toward 9,662"));
		String pull = MenuCopy.slotCall("Tooth half of key", false, 9300, 0, 458, q, OfferAlert.Kind.MARGIN_CRUSH);
		assertTrue(pull, pull.startsWith("✕ Pull"));
	}
}
