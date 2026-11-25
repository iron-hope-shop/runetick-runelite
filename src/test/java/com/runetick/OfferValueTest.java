package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Test;

public class OfferValueTest
{
	private static GrandExchangeOffer offer(GrandExchangeOfferState state, int total, int filled, long price, long spent)
	{
		GrandExchangeOffer offer = mock(GrandExchangeOffer.class);
		when(offer.getState()).thenReturn(state);
		when(offer.getItemId()).thenReturn(4151);
		when(offer.getTotalQuantity()).thenReturn(total);
		when(offer.getQuantitySold()).thenReturn(filled);
		when(offer.getPrice()).thenReturn(price);
		when(offer.getSpent()).thenReturn(spent);
		return offer;
	}

	@Test
	public void runningBuyCountsOnlyTheLockedCoinsSoCollectedFillsAreNotCountedTwice()
	{
		assertEquals(7_000L, OfferValue.one(offer(GrandExchangeOfferState.BUYING, 10, 3, 1000L, 3000L), id -> 1200L));
	}

	@Test
	public void runningSellCountsOnlyTheUnsoldItems()
	{
		assertEquals(6 * 1400L, OfferValue.one(offer(GrandExchangeOfferState.SELLING, 10, 4, 1500L, 5_880L), id -> 1400L));
	}

	@Test
	public void finishedBuyHoldsTheItemsAndTheRefundUntilCollected()
	{
		// 10 bought for 9,500 of the 10,000 put up: items at market, 500 back.
		assertEquals(10 * 1200L + 500L, OfferValue.one(offer(GrandExchangeOfferState.BOUGHT, 10, 10, 1000L, 9_500L), id -> 1200L));
	}

	@Test
	public void cancelledBuyHoldsWhatFilledAndTheUnspentCoins()
	{
		assertEquals(3 * 1200L + 7_000L, OfferValue.one(offer(GrandExchangeOfferState.CANCELLED_BUY, 10, 3, 1000L, 3000L), id -> 1200L));
	}

	@Test
	public void finishedSellHoldsTheCoins()
	{
		assertEquals(14_700L, OfferValue.one(offer(GrandExchangeOfferState.SOLD, 10, 10, 1500L, 14_700L), id -> 1400L));
	}

	@Test
	public void cancelledSellHoldsTheUnsoldItemsAndTheCoins()
	{
		assertEquals(6 * 1400L + 5_880L, OfferValue.one(offer(GrandExchangeOfferState.CANCELLED_SELL, 10, 4, 1500L, 5_880L), id -> 1400L));
	}

	@Test
	public void withoutAMarketPriceTheOfferPriceIsUsed()
	{
		assertEquals(6 * 1500L, OfferValue.one(offer(GrandExchangeOfferState.SELLING, 10, 4, 1500L, 5_880L), id -> 0L));
	}

	@Test
	public void emptyIsZero()
	{
		GrandExchangeOffer offer = mock(GrandExchangeOffer.class);
		when(offer.getState()).thenReturn(GrandExchangeOfferState.EMPTY);
		assertEquals(0, OfferValue.one(offer, id -> 999));
	}
}
