package com.runetick;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
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

	@Test
	public void missingInventoryDoesNotWipeCash()
	{
		Client client = mock(Client.class);
		CashStore store = mock(CashStore.class);
		when(store.load()).thenReturn(null);
		CashTracker tracker = new CashTracker(client, store);

		ItemContainer bag = mock(ItemContainer.class);
		when(bag.count(995)).thenReturn(1_000);
		when(bag.count(13204)).thenReturn(0);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(bag);
		tracker.refreshInventory();
		assertEquals(1_000L, tracker.getInventoryGp());

		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(null);
		tracker.refreshInventory();
		assertEquals(1_000L, tracker.getInventoryGp());
	}
}
