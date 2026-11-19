package com.runetick;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.GrandExchangeOfferState;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class OfferSyncTest
{
	private static final int WHIP = 4151;

	private RuneTickApi api;
	private RuneTickConfig config;
	private FirebaseSession session;
	private final Map<Integer, String> loggedBySlot = new HashMap<>();
	private OfferSync sync;

	@Before
	public void setUp() throws Exception
	{
		api = mock(RuneTickApi.class);
		when(api.getPlaced()).thenReturn(Collections.emptyList());

		config = mock(RuneTickConfig.class);
		when(config.syncPlaced()).thenReturn(true);
		when(config.logTrades()).thenReturn(true);

		session = mock(FirebaseSession.class);
		when(session.isLinked()).thenReturn(true);

		sync = new OfferSync(api, config, session, new OfferSync.LoggedOffers()
		{
			@Override
			public String get(int slot)
			{
				return loggedBySlot.get(slot);
			}

			@Override
			public void put(int slot, String fingerprint)
			{
				loggedBySlot.put(slot, fingerprint);
			}

			@Override
			public void clear(int slot)
			{
				loggedBySlot.remove(slot);
			}
		});
	}

	private static OfferEvent offer(int slot, GrandExchangeOfferState state, int total, int filled, long spent)
	{
		return new OfferEvent(slot, WHIP, state, 100, total, filled, spent, 1L);
	}

	private static RuneTickApi.PlacedFlip flip(String id, int itemId, int units, int bought, int sold)
	{
		RuneTickApi.PlacedFlip f = new RuneTickApi.PlacedFlip();
		f.setId(id);
		f.setItemId(itemId);
		f.setUnits(units);
		f.setBought(bought);
		f.setSold(sold);
		return f;
	}

	@Test
	public void doesNothingWhenNotConnectedOrNothingFilled() throws Exception
	{
		assertEquals(OfferSync.Result.IGNORED, sync.handle(offer(0, GrandExchangeOfferState.BUYING, 10, 0, 0), "Abyssal whip"));

		when(session.isLinked()).thenReturn(false);
		assertEquals(OfferSync.Result.IGNORED, sync.handle(offer(0, GrandExchangeOfferState.BOUGHT, 10, 10, 1000), "Abyssal whip"));

		verifyNoMoreInteractions(api);
	}

	@Test
	public void buyProgressUpdatesPlacedFlipAndIsNotLoggedAsTrade() throws Exception
	{
		when(api.getPlaced()).thenReturn(Arrays.asList(flip("other", 2, 10, 0, 0), flip("a", WHIP, 10, 2, 0)));

		assertEquals(OfferSync.Result.PLACED_UPDATED, sync.handle(offer(0, GrandExchangeOfferState.BUYING, 10, 6, 600), "Abyssal whip"));
		verify(api).updatePlaced("a", 6, null);

		assertEquals(OfferSync.Result.PLACED_UPDATED, sync.handle(offer(0, GrandExchangeOfferState.BOUGHT, 10, 10, 1000), "Abyssal whip"));
		verify(api).updatePlaced("a", 10, null);

		verify(api, never()).logTrade(anyInt(), anyString(), anyBoolean(), anyInt(), anyLong());
	}

	@Test
	public void placedCountsNeverGoDownAndAreCappedAtUnits() throws Exception
	{
		when(api.getPlaced()).thenReturn(Collections.singletonList(flip("a", WHIP, 10, 6, 0)));

		// stale re-send after login
		assertEquals(OfferSync.Result.IGNORED, sync.handle(offer(0, GrandExchangeOfferState.BUYING, 10, 6, 600), "Abyssal whip"));
		assertEquals(OfferSync.Result.IGNORED, sync.handle(offer(0, GrandExchangeOfferState.BUYING, 10, 3, 300), "Abyssal whip"));
		verify(api, never()).updatePlaced(any(), any(), any());

		// bought more in game than the flip was placed for
		assertEquals(OfferSync.Result.PLACED_UPDATED, sync.handle(offer(0, GrandExchangeOfferState.BOUGHT, 50, 50, 5000), "Abyssal whip"));
		verify(api).updatePlaced("a", 10, null);
	}

	@Test
	public void sellProgressUpdatesSoldAndCatchesBoughtUp() throws Exception
	{
		when(api.getPlaced()).thenReturn(Collections.singletonList(flip("a", WHIP, 10, 10, 0)));
		assertEquals(OfferSync.Result.PLACED_UPDATED, sync.handle(offer(1, GrandExchangeOfferState.SELLING, 10, 4, 480), "Abyssal whip"));
		verify(api).updatePlaced("a", null, 4);

		// the buy happened before the plugin was watching
		when(api.getPlaced()).thenReturn(Collections.singletonList(flip("b", WHIP, 10, 0, 0)));
		assertEquals(OfferSync.Result.PLACED_UPDATED, sync.handle(offer(1, GrandExchangeOfferState.SOLD, 10, 10, 1200), "Abyssal whip"));
		verify(api).updatePlaced("b", 10, 10);
	}

	@Test
	public void picksOldestFlipStillWaitingOnThatSide() throws Exception
	{
		// newest first, as the API returns them
		when(api.getPlaced()).thenReturn(Arrays.asList(
			flip("new", WHIP, 10, 0, 0),
			flip("old-selling", WHIP, 10, 10, 2),
			flip("done", WHIP, 10, 10, 10)));

		sync.handle(offer(0, GrandExchangeOfferState.BUYING, 10, 5, 500), "Abyssal whip");
		verify(api).updatePlaced("new", 5, null);

		sync.handle(offer(1, GrandExchangeOfferState.SELLING, 10, 5, 600), "Abyssal whip");
		verify(api).updatePlaced("old-selling", null, 5);
	}

	@Test
	public void finishedOfferWithoutPlacedFlipIsLoggedOnce() throws Exception
	{
		OfferEvent bought = offer(2, GrandExchangeOfferState.BOUGHT, 10, 10, 995);

		assertEquals(OfferSync.Result.TRADE_LOGGED, sync.handle(bought, "Abyssal whip"));
		// re-sent on the next login while still uncollected
		assertEquals(OfferSync.Result.IGNORED, sync.handle(bought, "Abyssal whip"));

		// 995 / 10 rounds to 100 each
		verify(api, times(1)).logTrade(WHIP, "Abyssal whip", true, 10, 100);

		// collected, then an identical offer in the same slot is a new trade
		sync.slotEmptied(2);
		assertEquals(OfferSync.Result.TRADE_LOGGED, sync.handle(bought, "Abyssal whip"));
		verify(api, times(2)).logTrade(WHIP, "Abyssal whip", true, 10, 100);
	}

	@Test
	public void partFilledOffersAreLoggedOnlyOnceCancelled() throws Exception
	{
		assertEquals(OfferSync.Result.IGNORED, sync.handle(offer(0, GrandExchangeOfferState.SELLING, 10, 4, 480), "Abyssal whip"));
		verify(api, never()).logTrade(anyInt(), anyString(), anyBoolean(), anyInt(), anyLong());

		assertEquals(OfferSync.Result.TRADE_LOGGED, sync.handle(offer(0, GrandExchangeOfferState.CANCELLED_SELL, 10, 4, 480), "Abyssal whip"));
		verify(api).logTrade(WHIP, "Abyssal whip", false, 4, 120);
	}

	@Test
	public void failedTradeLogIsRetriedNextTime() throws Exception
	{
		OfferEvent sold = offer(0, GrandExchangeOfferState.SOLD, 1, 1, 120);
		org.mockito.Mockito.doThrow(new java.io.IOException("offline")).doNothing()
			.when(api).logTrade(anyInt(), anyString(), anyBoolean(), anyInt(), anyLong());

		try
		{
			sync.handle(sold, "Abyssal whip");
		}
		catch (java.io.IOException expected)
		{
			// expected
		}
		assertEquals(OfferSync.Result.TRADE_LOGGED, sync.handle(sold, "Abyssal whip"));
	}

	@Test
	public void respectsSettings() throws Exception
	{
		when(api.getPlaced()).thenReturn(Collections.singletonList(flip("a", WHIP, 10, 0, 0)));
		when(config.syncPlaced()).thenReturn(false);
		when(config.logTrades()).thenReturn(false);

		assertEquals(OfferSync.Result.IGNORED, sync.handle(offer(0, GrandExchangeOfferState.BOUGHT, 10, 10, 1000), "Abyssal whip"));
		verify(api, never()).getPlaced();
		verify(api, never()).updatePlaced(any(), any(), any());
		verify(api, never()).logTrade(anyInt(), anyString(), anyBoolean(), anyInt(), anyLong());

		// placed sync off, trade log on: logged as a plain trade
		when(config.logTrades()).thenReturn(true);
		assertEquals(OfferSync.Result.TRADE_LOGGED, sync.handle(offer(0, GrandExchangeOfferState.BOUGHT, 10, 10, 1000), "Abyssal whip"));
	}
}
