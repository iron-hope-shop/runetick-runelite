package com.runetick;

import static org.junit.Assert.assertEquals;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ItemValueTest
{
	@Test
	public void sumsQtyTimesPrice()
	{
		Map<Integer, Integer> qty = new HashMap<>();
		qty.put(4151, 2);
		qty.put(2, 10);
		assertEquals(2_050L, ItemValue.estimate(qty, id -> id == 4151 ? 1000 : 5));
	}

	@Test
	public void emptyIsZero()
	{
		assertEquals(0, ItemValue.estimate(Collections.emptyMap(), id -> 100));
		assertEquals(0, ItemValue.estimate(null, id -> 100));
	}
}
