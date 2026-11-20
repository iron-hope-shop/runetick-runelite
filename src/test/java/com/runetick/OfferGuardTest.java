package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

public class OfferGuardTest
{
	private RuneTickApi api;
	private RuneTickConfig config;
	private OfferGuard guard;

	@Before
	public void setUp() throws Exception
	{
		api = mock(RuneTickApi.class);
		config = mock(RuneTickConfig.class);
		when(config.offerEta()).thenReturn(true);
		when(config.offerPriceAlerts()).thenReturn(true);
		when(config.desktopNotify()).thenReturn(true);
		guard = new OfferGuard(api, config);
	}

	@Test
	public void pollBuildsEtaAndDedupesAlerts() throws Exception
	{
		RuneTickApi.ItemQuote quote = new RuneTickApi.ItemQuote();
		quote.setId(4151);
		quote.setName("Abyssal whip");
		quote.setBuy(1100);
		quote.setSell(1000);
		quote.setAfterTaxSell(980);
		quote.setVolume(5);
		when(api.getItem(4151)).thenReturn(quote);

		RuneTickApi.GeLiveOffer offer = new RuneTickApi.GeLiveOffer();
		offer.setSlot(0);
		offer.setItemId(4151);
		offer.setItemName("Abyssal whip");
		offer.setSide("buy");
		offer.setPrice(1000);
		offer.setTotal(60);
		offer.setFilled(0);

		AtomicInteger notifies = new AtomicInteger();
		guard.poll(Collections.singletonList(offer), msg -> notifies.incrementAndGet());
		assertEquals(1, notifies.get());
		assertEquals(1, guard.getLiveRows().size());
		assertEquals("~1h", guard.getLiveRows().get(0).eta);
		assertEquals(OfferAlert.Kind.BUY_RAISE, guard.getLiveRows().get(0).alert);

		// Same adverse condition — no second notify.
		guard.poll(Collections.singletonList(offer), msg -> notifies.incrementAndGet());
		assertEquals(1, notifies.get());

		// Empty offers clears fired set for next open.
		guard.poll(Collections.emptyList(), msg -> notifies.incrementAndGet());
		assertTrue(guard.getLiveRows().isEmpty());
		assertEquals(1, notifies.get());
	}

	@Test
	public void etaForUsesCachedVolume() throws Exception
	{
		RuneTickApi.ItemQuote quote = new RuneTickApi.ItemQuote();
		quote.setId(1);
		quote.setVolume(10);
		when(api.getItem(anyInt())).thenReturn(quote);

		RuneTickApi.GeLiveOffer offer = new RuneTickApi.GeLiveOffer();
		offer.setSlot(1);
		offer.setItemId(1);
		offer.setItemName("A");
		offer.setSide("sell");
		offer.setPrice(100);
		offer.setTotal(120);
		offer.setFilled(0);

		guard.poll(Collections.singletonList(offer), msg -> { });
		assertEquals("~1h", guard.etaFor(1, 120));
	}

	@Test
	public void respectsConfigToggles() throws Exception
	{
		when(config.offerEta()).thenReturn(false);
		when(config.offerPriceAlerts()).thenReturn(false);

		RuneTickApi.ItemQuote quote = new RuneTickApi.ItemQuote();
		quote.setId(2);
		quote.setBuy(2000);
		quote.setVolume(50);
		when(api.getItem(2)).thenReturn(quote);

		RuneTickApi.GeLiveOffer offer = new RuneTickApi.GeLiveOffer();
		offer.setSlot(3);
		offer.setItemId(2);
		offer.setItemName("B");
		offer.setSide("buy");
		offer.setPrice(1000);
		offer.setTotal(10);
		offer.setFilled(0);

		List<String> msgs = new ArrayList<>();
		guard.poll(Collections.singletonList(offer), msgs::add);
		assertTrue(msgs.isEmpty());
		assertEquals("", guard.getLiveRows().get(0).eta);
	}
}
