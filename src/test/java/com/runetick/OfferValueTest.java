package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Test;

public class OfferValueTest
{
	@Test
	public void buyCountsLockedCashAndFilledItems()
	{
		GrandExchangeOffer offer = mock(GrandExchangeOffer.class);
		when(offer.getState()).thenReturn(GrandExchangeOfferState.BUYING);
		when(offer.getItemId()).thenReturn(4151);
		when(offer.getTotalQuantity()).thenReturn(10);
		when(offer.getQuantitySold()).thenReturn(3);
		when(offer.getPrice()).thenReturn(1000L);
		when(offer.getSpent()).thenReturn(3000L);

		// 7 left * 1000 locked + 3 filled * market 1200
		assertEquals(7_000L + 3_600L, OfferValue.one(offer, id -> 1200L));
	}

	@Test
	public void sellCountsUnsoldItemsAndSpentCoins()
	{
		GrandExchangeOffer offer = mock(GrandExchangeOffer.class);
		when(offer.getState()).thenReturn(GrandExchangeOfferState.SELLING);
		when(offer.getItemId()).thenReturn(4151);
		when(offer.getTotalQuantity()).thenReturn(10);
		when(offer.getQuantitySold()).thenReturn(4);
		when(offer.getPrice()).thenReturn(1500L);
		when(offer.getSpent()).thenReturn(5_880L); // after-tax proceeds waiting

		// 6 unsold * market 1400 + 5880 coins
		assertEquals(6 * 1400L + 5_880L, OfferValue.one(offer, id -> 1400L));
	}

	@Test
	public void emptyIsZero()
	{
		GrandExchangeOffer offer = mock(GrandExchangeOffer.class);
		when(offer.getState()).thenReturn(GrandExchangeOfferState.EMPTY);
		assertEquals(0, OfferValue.one(offer, id -> 999));
	}
}
