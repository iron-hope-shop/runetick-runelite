package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BankViewTest
{
	@Test
	public void taxIsTwoPercentCappedAndSkipsCheapItems()
	{
		assertEquals(49, BankView.afterTax(49));
		assertEquals(98, BankView.afterTax(100));
		assertEquals(980_000, BankView.afterTax(1_000_000));
		assertEquals(995_000_000, BankView.afterTax(1_000_000_000));
	}

	@Test
	public void stackValuesAreShortEnoughForAnItemSlot()
	{
		assertEquals("950", BankView.compact(950));
		assertEquals("340K", BankView.compact(340_900));
		assertEquals("1.2M", BankView.compact(1_250_000));
		assertEquals("12M", BankView.compact(12_000_000));
		assertEquals("2.1B", BankView.compact(2_147_000_000L));
	}

	@Test
	public void highValueKeepsOnlyStacksWorthTheMinimum()
	{
		Map<Integer, Integer> qty = new HashMap<>();
		qty.put(1, 10_000);
		qty.put(2, 3);
		qty.put(3, 5);
		Map<Integer, Long> price = new HashMap<>();
		price.put(1, 150L);
		price.put(2, 40_000_000L);
		price.put(3, 100L);
		Map<Integer, BankView.Mark> marks = BankView.valueMarks(qty, id -> price.getOrDefault(id, 0L), 1_000_000);
		assertEquals("1.5M", marks.get(1).label);
		assertEquals("120M", marks.get(2).label);
		assertNull(marks.get(3));
		assertTrue(BankView.valueMarks(qty, id -> 0L, 1).isEmpty());
	}

	@Test
	public void toSellComparesTheTaxedSellWithWhatWasPaid()
	{
		Map<Integer, Long> price = new HashMap<>();
		price.put(1, 1_200L);
		price.put(2, 1_000L);
		price.put(3, 500L);
		Map<Integer, Long> cost = new HashMap<>();
		cost.put(1, 1_000L);
		cost.put(2, 1_000L);
		Map<Integer, BankView.Mark> marks = BankView.profitMarks(
			Arrays.asList(1, 2, 3), id -> price.getOrDefault(id, 0L), id -> cost.getOrDefault(id, 0L));
		assertEquals("+18%", marks.get(1).label);
		assertEquals(1, marks.get(1).tone);
		assertEquals("-2%", marks.get(2).label);
		assertEquals(-1, marks.get(2).tone);
		assertFalse(marks.containsKey(3));
	}

	@Test
	public void theBankButtonCyclesThroughTheThreeViews()
	{
		assertEquals(BankView.Mode.VALUE, BankView.Mode.OFF.next());
		assertEquals(BankView.Mode.SELL, BankView.Mode.VALUE.next());
		assertEquals(BankView.Mode.OFF, BankView.Mode.SELL.next());
		assertEquals("To sell", BankView.Mode.SELL.toString());
	}
}
