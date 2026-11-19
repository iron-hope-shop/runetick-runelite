package com.runetick;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Test;

public class CashTrackerTest
{
	@Test
	public void countsCoinsAndPlatinum()
	{
		ItemContainer bag = mock(ItemContainer.class);
		when(bag.count(995)).thenReturn(12_345);
		when(bag.count(13204)).thenReturn(2);
		assertEquals(12_345 + 2_000L, CashTracker.countGp(bag));
	}

	@Test
	public void nullContainerIsZero()
	{
		assertEquals(0, CashTracker.countGp(null));
	}
}
