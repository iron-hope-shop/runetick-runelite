package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DayTradeCopyTest
{
	@Test
	public void describesCheapAndDear()
	{
		RuneTickApi.Signal cheap = new RuneTickApi.Signal();
		cheap.setSide("cheap");
		cheap.setName("Opal ring");
		cheap.setBuy(776);
		cheap.setSell(820);
		cheap.setZScore24h(-2.1);
		cheap.setHint("mean reversion");

		assertEquals("Buy dip", DayTradeCopy.action(cheap));
		assertTrue(DayTradeCopy.reason(cheap).contains("dip"));
		assertTrue(DayTradeCopy.prices(cheap).startsWith("buy <="));
		assertEquals("mean reversion", DayTradeCopy.hint(cheap));

		RuneTickApi.Signal dear = new RuneTickApi.Signal();
		dear.setSide("dear");
		dear.setBuy(100);
		dear.setSell(140);
		dear.setZScore24h(1.8);
		assertEquals("Sell spike", DayTradeCopy.action(dear));
		assertTrue(DayTradeCopy.prices(dear).startsWith("sell >="));
	}
}
