package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RuneTickApiTest
{
	private final Gson gson = new Gson();
	private MockWebServer site;
	private FirebaseSession session;
	private RuneTickApi api;

	@Before
	public void setUp() throws Exception
	{
		site = new MockWebServer();
		site.start();

		RuneTickConfig config = mock(RuneTickConfig.class);
		// trailing slash on purpose: the address is typed by hand
		when(config.siteUrl()).thenReturn(site.url("/").toString());

		session = mock(FirebaseSession.class);
		when(session.getIdToken()).thenReturn("id-token");
		when(session.isLinked()).thenReturn(true);

		api = new RuneTickApi(new OkHttpClient(), gson, config, session);
	}

	@After
	public void tearDown() throws Exception
	{
		site.shutdown();
	}

	@Test
	public void readsPlacedFlips() throws Exception
	{
		site.enqueue(new MockResponse().setBody("{\"placed\":["
			+ "{\"id\":\"a\",\"itemId\":4151,\"itemName\":\"Abyssal whip\",\"units\":10,\"bid\":100,\"ask\":120,\"bought\":4,\"sold\":1,\"status\":\"buying\"},"
			+ "{\"itemId\":2}"
			+ "],\"summary\":{\"open\":1}}"));

		List<RuneTickApi.PlacedFlip> placed = api.getPlaced();

		assertEquals(1, placed.size());
		RuneTickApi.PlacedFlip flip = placed.get(0);
		assertEquals("a", flip.getId());
		assertEquals(4151, flip.getItemId());
		assertEquals(10, flip.getUnits());
		assertEquals(4, flip.getBought());
		assertEquals(1, flip.getSold());

		RecordedRequest request = site.takeRequest();
		assertEquals("GET", request.getMethod());
		assertEquals("/api/v1/me/placed", request.getPath());
		assertEquals("Bearer id-token", request.getHeader("Authorization"));
	}

	@Test
	public void patchesOnlyTheGivenCounts() throws Exception
	{
		site.enqueue(new MockResponse().setBody("{}"));
		site.enqueue(new MockResponse().setBody("{}"));

		api.updatePlaced("a", 7, null);
		api.updatePlaced("a", 9, 9);

		RecordedRequest first = site.takeRequest();
		assertEquals("PATCH", first.getMethod());
		assertEquals("/api/v1/me/placed/a", first.getPath());
		assertTrue(first.getHeader("Content-Type").startsWith("application/json"));
		JsonObject body = gson.fromJson(first.getBody().readUtf8(), JsonObject.class);
		assertEquals(7, body.get("bought").getAsInt());
		assertFalse(body.has("sold"));

		JsonObject second = gson.fromJson(site.takeRequest().getBody().readUtf8(), JsonObject.class);
		assertEquals(9, second.get("bought").getAsInt());
		assertEquals(9, second.get("sold").getAsInt());
	}

	@Test
	public void logsTrade() throws Exception
	{
		site.enqueue(new MockResponse().setResponseCode(201).setBody("{}"));

		api.logTrade(4151, "Abyssal whip", false, 3, 1_500_000);

		RecordedRequest request = site.takeRequest();
		assertEquals("POST", request.getMethod());
		assertEquals("/api/v1/me/trades", request.getPath());
		JsonObject body = gson.fromJson(request.getBody().readUtf8(), JsonObject.class);
		assertEquals(4151, body.get("itemId").getAsInt());
		assertEquals("Abyssal whip", body.get("itemName").getAsString());
		assertEquals("sell", body.get("side").getAsString());
		assertEquals(3, body.get("qty").getAsInt());
		assertEquals(1_500_000, body.get("price").getAsLong());
	}

	@Test
	public void syncsHistoryAndCash() throws Exception
	{
		site.enqueue(new MockResponse().setBody(
			"{\"added\":1,\"cashUpdated\":true,\"tradeCount\":1,\"capitalGp\":5000,\"inventoryGp\":5000,\"bankGp\":0,\"bankSeen\":false,\"geSyncedAt\":1}"));

		GeTrade trade = gson.fromJson("{\"b\":true,\"i\":8790,\"q\":10,\"p\":350,\"t\":1000}", GeTrade.class);
		Map<Integer, String> names = new HashMap<>();
		names.put(8790, "Candle");

		RuneTickApi.GeLiveOffer live = new RuneTickApi.GeLiveOffer();
		live.setSlot(0);
		live.setItemId(8790);
		live.setItemName("Candle");
		live.setSide("buy");
		live.setState("BUYING");
		live.setPrice(350);
		live.setTotal(10);
		live.setFilled(4);
		live.setSpent(1400);

		RuneTickApi.NetWorthSnap nw = new RuneTickApi.NetWorthSnap();
		nw.setCashGp(5000);
		nw.setInventoryCashGp(5000);
		nw.setInvItemsGp(100);
		nw.setBankItemsGp(0);
		nw.setOffersGp(1400);
		nw.setInvGeGp(6500);
		nw.setTotalGp(6500);
		nw.setBankItemsSeen(false);
		nw.setSessionArmed(false);
		nw.setAt(1);

		RuneTickApi.SyncResult result = api.syncLocal(5000, 0, false,
			Collections.singletonList(trade), names,
			Collections.singletonList(live), nw);

		assertEquals(1, result.getAdded());
		RecordedRequest request = site.takeRequest();
		assertEquals("POST", request.getMethod());
		assertEquals("/api/v1/me/sync", request.getPath());
		JsonObject body = gson.fromJson(request.getBody().readUtf8(), JsonObject.class);
		assertEquals(5000, body.get("inventoryGp").getAsLong());
		assertFalse(body.has("capitalGp"));
		JsonArray trades = body.getAsJsonArray("trades");
		assertEquals(1, trades.size());
		assertEquals("Candle", trades.get(0).getAsJsonObject().get("itemName").getAsString());
		assertEquals("buy", trades.get(0).getAsJsonObject().get("side").getAsString());
		JsonArray offers = body.getAsJsonArray("geOffers");
		assertEquals(1, offers.size());
		assertEquals(4, offers.get(0).getAsJsonObject().get("filled").getAsInt());
		assertEquals(6500, body.getAsJsonObject("netWorth").get("totalGp").getAsLong());
	}

	@Test
	public void refreshesTokenOnceAfter401() throws Exception
	{
		when(session.getIdToken()).thenReturn("stale", "fresh");
		site.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_token\"}"));
		site.enqueue(new MockResponse().setBody("{\"placed\":[]}"));

		assertTrue(api.getPlaced().isEmpty());

		verify(session, times(1)).invalidateIdToken();
		assertEquals("Bearer stale", site.takeRequest().getHeader("Authorization"));
		assertEquals("Bearer fresh", site.takeRequest().getHeader("Authorization"));
	}

	@Test
	public void readsPlanItemAndSignals() throws Exception
	{
		site.enqueue(new MockResponse().setBody("{\"pace\":\"fast\",\"expectedGpPerHour\":12000,\"positions\":["
			+ "{\"id\":4151,\"name\":\"Abyssal whip\",\"bid\":100,\"ask\":120,\"units\":10,\"gpPerHour\":5000,\"pFill\":0.5,\"marginPerUnit\":18,\"market\":\"ranging\"}"
			+ "],\"fat\":[{\"id\":9,\"name\":\"Fat\",\"bid\":1000,\"ask\":1500,\"marginPerUnit\":470,\"dreamUnits\":100,\"dreamGp\":47000,\"market\":\"volatile\"}]}"));
		RuneTickApi.Plan plan = api.getPlan(50_000_000, 6, "fast");
		assertEquals("fast", plan.getPace());
		assertEquals(1, plan.getPositions().size());
		assertEquals(4151, plan.getPositions().get(0).getId());
		assertEquals(100, plan.getPositions().get(0).getBid());
		assertEquals(120, plan.getPositions().get(0).getAsk());
		assertEquals(1, plan.getFat().size());
		assertEquals(47000, plan.getFat().get(0).getDreamGp());
		RecordedRequest planReq = site.takeRequest();
		assertTrue(planReq.getPath().startsWith("/api/v1/plan?"));
		assertTrue(planReq.getPath().contains("pace=fast"));
		assertTrue(planReq.getPath().contains("capital=50000000"));

		site.enqueue(new MockResponse().setBody("{\"id\":4151,\"name\":\"Abyssal whip\",\"buy\":622112,\"sell\":637541,\"afterTaxSell\":624791,\"limit\":70,\"volume\":77,"
			+ "\"analytics\":{\"fillProb\":0.56,\"market\":{\"label\":\"quiet\",\"hint\":\"slow\"}}}"));
		RuneTickApi.ItemQuote quote = api.getItem(4151);
		assertEquals(4151, quote.getId());
		assertEquals(622112, quote.getBuy());
		assertEquals(637541, quote.getSell());
		assertEquals("quiet", quote.getAnalytics().getMarket().getLabel());
		assertEquals("/api/v1/items/4151", site.takeRequest().getPath());

		site.enqueue(new MockResponse().setBody("{\"dips\":[{\"id\":1,\"name\":\"Dip\",\"price\":650,\"median30\":1559,\"dip\":0.493,\"alchFloor\":true,\"dailyVolume\":1677,\"limit\":125}],"
			+ "\"evidence\":\"buy dips\"}"));
		RuneTickApi.DipsResult dips = api.getDips(8);
		assertEquals(1, dips.getDips().size());
		assertEquals(650, dips.getDips().get(0).getPrice());
		assertTrue(dips.getDips().get(0).isAlchFloor());
		assertTrue(site.takeRequest().getPath().startsWith("/api/v1/dips"));
	}

	@Test
	public void publicItemWorksWithoutSignIn() throws Exception
	{
		when(session.isLinked()).thenReturn(false);
		site.enqueue(new MockResponse().setBody("{\"id\":2,\"name\":\"Cannonball\",\"buy\":100,\"sell\":110}"));

		RuneTickApi.ItemQuote quote = api.getItem(2);
		assertEquals(2, quote.getId());
		RecordedRequest request = site.takeRequest();
		assertEquals("/api/v1/items/2", request.getPath());
		assertEquals(null, request.getHeader("Authorization"));
	}

	@Test
	public void missingItemReturnsNull() throws Exception
	{
		site.enqueue(new MockResponse().setResponseCode(404).setBody("{\"error\":\"not_found\"}"));
		assertEquals(null, api.getItem(21082));
	}

	@Test
	public void failsOnRepeated401AndServerErrors() throws Exception
	{
		site.enqueue(new MockResponse().setResponseCode(401));
		site.enqueue(new MockResponse().setResponseCode(401));
		try
		{
			api.getPlaced();
			fail("expected IOException");
		}
		catch (IOException e)
		{
			assertTrue(e.getMessage().contains("HTTP 401"));
		}
		assertEquals(2, site.getRequestCount());

		site.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"trade_failed\"}"));
		try
		{
			api.logTrade(1, "x", true, 1, 1);
			fail("expected IOException");
		}
		catch (IOException e)
		{
			assertTrue(e.getMessage().contains("HTTP 500"));
			assertTrue(e.getMessage().contains("trade_failed"));
		}
	}
}
