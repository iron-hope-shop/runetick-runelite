package com.runetick;

import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OfferTrackerTest
{
	private static final int ABYSSAL_WHIP = 4151;

	private final OfferTracker tracker = new OfferTracker();

	static GrandExchangeOffer offer(GrandExchangeOfferState state, int itemId, long price, int total, int filled, long spent)
	{
		GrandExchangeOffer offer = mock(GrandExchangeOffer.class);
		when(offer.getState()).thenReturn(state);
		when(offer.getItemId()).thenReturn(itemId);
		when(offer.getPrice()).thenReturn(price);
		when(offer.getTotalQuantity()).thenReturn(total);
		when(offer.getQuantitySold()).thenReturn(filled);
		when(offer.getSpent()).thenReturn(spent);
		return offer;
	}

	@Test
	public void mapsOfferFields()
	{
		OfferEvent event = tracker.track(3, offer(GrandExchangeOfferState.BUYING, ABYSSAL_WHIP, 1_500_000, 2, 1, 1_499_000), 42L);

		assertNotNull(event);
		assertEquals(3, event.getSlot());
		assertEquals(ABYSSAL_WHIP, event.getItemId());
		assertEquals(GrandExchangeOfferState.BUYING, event.getState());
		assertEquals(1_499_000, event.unitPrice());
		assertEquals(1_500_000, event.getPrice());
		assertEquals(2, event.getTotalQuantity());
		assertEquals(1, event.getQuantityFilled());
		assertEquals(1_499_000, event.getSpent());
		assertEquals(42L, event.getTimestamp());
	}

	@Test
	public void ignoresEmptySlotsAndNulls()
	{
		assertNull(tracker.track(0, offer(GrandExchangeOfferState.EMPTY, 0, 0, 0, 0, 0), 1L));
		assertNull(tracker.track(0, null, 1L));
	}

	@Test
	public void ignoresRepeatedUpdate()
	{
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.BUYING, ABYSSAL_WHIP, 100, 10, 0, 0), 1L));
		assertNull(tracker.track(0, offer(GrandExchangeOfferState.BUYING, ABYSSAL_WHIP, 100, 10, 0, 0), 2L));
	}

	@Test
	public void reportsProgressAndOtherSlots()
	{
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.BUYING, ABYSSAL_WHIP, 100, 10, 0, 0), 1L));
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.BUYING, ABYSSAL_WHIP, 100, 10, 5, 500), 2L));
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.BOUGHT, ABYSSAL_WHIP, 100, 10, 10, 1000), 3L));
		assertNotNull(tracker.track(1, offer(GrandExchangeOfferState.BOUGHT, ABYSSAL_WHIP, 100, 10, 10, 1000), 4L));
	}

	@Test
	public void sameOfferReportedAgainAfterSlotEmptiedOrReset()
	{
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.SELLING, ABYSSAL_WHIP, 100, 10, 0, 0), 1L));
		assertNull(tracker.track(0, offer(GrandExchangeOfferState.EMPTY, 0, 0, 0, 0, 0), 2L));
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.SELLING, ABYSSAL_WHIP, 100, 10, 0, 0), 3L));

		tracker.reset();
		assertNotNull(tracker.track(0, offer(GrandExchangeOfferState.SELLING, ABYSSAL_WHIP, 100, 10, 0, 0), 4L));
	}
}
