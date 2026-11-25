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

	@Test
	public void leavingTheBankOutKeepsBankGoldOutOfTheTotalAndStopsReadingIt()
	{
		Client client = mock(Client.class);
		CashStore store = mock(CashStore.class);
		CashStore.Snapshot snap = new CashStore.Snapshot();
		snap.setInventoryGp(1_000);
		snap.setBankGp(50_000_000);
		snap.setBankSeen(true);
		when(store.load()).thenReturn(snap);
		CashTracker tracker = new CashTracker(client, store);
		assertEquals(50_001_000L, tracker.getTotalKnownGp());
		tracker.setCountBank(false);
		assertEquals(1_000L, tracker.getTotalKnownGp());
		ItemContainer bank = mock(ItemContainer.class);
		when(bank.count(995)).thenReturn(9);
		when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
		tracker.refreshBank();
		assertEquals(50_000_000L, tracker.getBankGp());
	}

	@Test
	public void anotherCharacterStartsFromItsOwnCash()
	{
		Client client = mock(Client.class);
		CashStore store = mock(CashStore.class);
		CashStore.Snapshot main = new CashStore.Snapshot();
		main.setBankGp(80_000_000);
		main.setBankSeen(true);
		when(store.load()).thenReturn(main);
		CashTracker tracker = new CashTracker(client, store);
		assertEquals(true, tracker.useAccount(111L));
		assertEquals(false, tracker.useAccount(111L));
		when(store.load()).thenReturn(null);
		assertEquals(true, tracker.useAccount(222L));
		assertEquals(0L, tracker.getBankGp());
		assertEquals(false, tracker.isBankSeen());
		org.mockito.Mockito.verify(store).useAccount(222L);
	}
}
