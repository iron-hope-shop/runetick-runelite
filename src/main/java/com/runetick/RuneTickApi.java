package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client for the Runetick API. Calls block; use a background thread.
 */
@Singleton
public class RuneTickApi
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	/** A flip the player placed in Runetick ("Your flips"). */
	@Data
	public static class PlacedFlip
	{
		private String id;
		private int itemId;
		private String itemName;
		private int units;
		private int bought;
		private int sold;
		private int bid;
		private int ask;
		private String status;
		private long profitSoFar;
		private long profitIfDone;
		private String next;
	}

	/** Totals across all of the player's placed flips. */
	@Data
	public static class PlacedSummary
	{
		private int open;
		private int done;
		private long profitSoFar;
		private long profitIfDone;
		private long profitToday;
		private int flipsToday;
	}

	@Data
	public static class PlacedResult
	{
		private List<PlacedFlip> placed = Collections.emptyList();
		private PlacedSummary summary;
	}

	@Data
	public static class Me
	{
		private String uid;
		private long capitalGp = 125_000_000L;
		private int slots = 8;
		private int checkinMinutes = 30;
		private long inventoryGp;
		private long bankGp;
		private boolean bankSeen;
		private long geSyncedAt;
		/** Site theme highlight, #rrggbb. */
		private String accent;
	}

	/** Live Grand Exchange slot for POST /v1/me/sync. */
	@Data
	public static class GeLiveOffer
	{
		private int slot;
		private int itemId;
		private String itemName;
		private String side;
		private String state;
		private long price;
		private int total;
		private int filled;
		private long spent;
	}

	/** Net worth snapshot for POST /v1/me/sync. */
	@Data
	public static class NetWorthSnap
	{
		private long cashGp;
		private long inventoryCashGp;
		private long invItemsGp;
		private long bankItemsGp;
		private long offersGp;
		/** Inv cash + inv items + GE offers (no bank). */
		private long invGeGp;
		private long totalGp;
		private boolean bankItemsSeen;
		private long sessionDeltaGp;
		private boolean sessionArmed;
		private long at;
	}

	/** Result of POST /v1/me/sync (GE history + cash + live offers + net worth). */
	@Data
	public static class SyncResult
	{
		private int added;
		private boolean cashUpdated;
		private int placedUpdated;
		private int tradeCount;
		private long capitalGp;
		private long inventoryGp;
		private long bankGp;
		private boolean bankSeen;
		private long geSyncedAt;
	}

	/** A recommended flip from /v1/plan. bid/ask are the prices to place. */
	@Data
	public static class PlanPosition
	{
		private int id;
		private String name;
		private int bid;
		private int ask;
		private int units;
		private long gpPerHour;
		private double pFill;
		private long marginPerUnit;
		private String market;
		private String marketHint;
		private double roi;
	}

	/** Fat after-tax gap — dream stack profit, not careful /h sizing. */
	@Data
	public static class FatSpread
	{
		private int id;
		private String name;
		private int bid;
		private int ask;
		private long marginPerUnit;
		private double roi;
		private int limit;
		private int dreamUnits;
		private long dreamGp;
		private int volume5m;
		private double clearsPerHour;
		private String market;
		private String marketHint;
	}

	/** Monthly-median dip from /v1/dips. */
	@Data
	public static class Dip
	{
		private int id;
		private String name;
		private long price;
		private long median30;
		private double dip;
		private boolean alchFloor;
		private long dailyVolume;
		private int limit;
	}

	@Data
	public static class DipsResult
	{
		private List<Dip> dips = Collections.emptyList();
		private String evidence = "";
	}

	@Data
	public static class Plan
	{
		private String pace;
		private long expectedGpPerHour;
		private long suggestedGpPerHour;
		private List<PlanPosition> positions = Collections.emptyList();
		private List<FatSpread> fat = Collections.emptyList();
	}

	/** Live market quote + analytics for one item. */
	@Data
	public static class ItemQuote
	{
		private int id;
		private String name;
		private long buy;
		private long sell;
		private long afterTaxSell;
		private int limit;
		private long volume;
		private Long alchBeatsSell;
		private Integer highalch;
		private Analytics analytics;
	}

	@Data
	public static class Analytics
	{
		private Double fillProb;
		private Double fastFillProb;
		private Double zScore24h;
		private Market market;
		private Anomaly anomaly;
		private Long expectedGpPerHour;
	}

	@Data
	public static class Market
	{
		private String label;
		private String hint;
		private Double halfLifeMin;
	}

	@Data
	public static class Anomaly
	{
		private List<String> flags;
		private String hint;
	}

	/** Mean-reversion day-trade signal. */
	@Data
	public static class Signal
	{
		private int id;
		private String name;
		private long buy;
		private long sell;
		private double zScore24h;
		private String market;
		private String side;
		private String hint;
	}

	@Data
	public static class Signals
	{
		private List<Signal> cheap = Collections.emptyList();
		private List<Signal> dear = Collections.emptyList();
	}

	private static class PlacedResponse
	{
		private List<PlacedFlip> placed;
		private PlacedSummary summary;
	}

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final RuneTickConfig config;
	private final FirebaseSession session;

	@Inject
	public RuneTickApi(OkHttpClient httpClient, Gson gson, RuneTickConfig config, FirebaseSession session)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.config = config;
		this.session = session;
	}

	public List<PlacedFlip> getPlaced() throws IOException
	{
		return getPlacedResult().getPlaced();
	}

	public PlacedResult getPlacedResult() throws IOException
	{
		String body = send("GET", "v1/me/placed", null, true);
		try
		{
			PlacedResponse parsed = gson.fromJson(body, PlacedResponse.class);
			PlacedResult result = new PlacedResult();
			if (parsed == null)
			{
				return result;
			}
			List<PlacedFlip> out = new ArrayList<>();
			if (parsed.placed != null)
			{
				for (PlacedFlip f : parsed.placed)
				{
					if (f != null && f.getId() != null)
					{
						out.add(f);
					}
				}
			}
			result.setPlaced(out);
			result.setSummary(parsed.summary);
			return result;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable placed flips from Runetick", e);
		}
	}

	public Me getMe() throws IOException
	{
		String body = send("GET", "v1/me", null, true);
		try
		{
			Me me = gson.fromJson(body, Me.class);
			return me == null ? new Me() : me;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable account from Runetick", e);
		}
	}

	public Plan getPlan(long capital, int positions, String pace) throws IOException
	{
		String path = "v1/plan?capital=" + capital + "&positions=" + positions
			+ (pace == null || pace.isEmpty() ? "" : "&pace=" + pace);
		String body = send("GET", path, null, true);
		try
		{
			Plan plan = gson.fromJson(body, Plan.class);
			if (plan == null)
			{
				return new Plan();
			}
			if (plan.getPositions() == null)
			{
				plan.setPositions(Collections.emptyList());
			}
			if (plan.getFat() == null)
			{
				plan.setFat(Collections.emptyList());
			}
			return plan;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable plan from Runetick", e);
		}
	}

	@Data
	public static class SearchHit
	{
		private int id;
		private String name;
		private long buy;
		private long sell;
	}

	@Data
	public static class SearchResult
	{
		private List<SearchHit> items = Collections.emptyList();
	}

	public List<SearchHit> search(String query) throws IOException
	{
		String q = query == null ? "" : query.trim();
		if (q.isEmpty())
		{
			return Collections.emptyList();
		}
		String encoded = java.net.URLEncoder.encode(q, "UTF-8");
		String body = send("GET", "v1/search?limit=6&q=" + encoded, null, false);
		try
		{
			SearchResult result = gson.fromJson(body, SearchResult.class);
			if (result == null || result.getItems() == null)
			{
				return Collections.emptyList();
			}
			return result.getItems();
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable search from Runetick", e);
		}
	}

	public DipsResult getDips(int limit) throws IOException
	{
		String body = send("GET", "v1/dips?limit=" + Math.max(1, limit), null, false);
		try
		{
			DipsResult out = gson.fromJson(body, DipsResult.class);
			if (out == null)
			{
				return new DipsResult();
			}
			if (out.getDips() == null)
			{
				out.setDips(Collections.emptyList());
			}
			if (out.getEvidence() == null)
			{
				out.setEvidence("");
			}
			return out;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable dips from Runetick", e);
		}
	}

	/**
	 * @return the quote, or null if Runetick has no mapping for this id (e.g. noted items)
	 */
	public ItemQuote getItem(int itemId) throws IOException
	{
		try
		{
			String body = send("GET", "v1/items/" + itemId, null, false);
			ItemQuote quote = gson.fromJson(body, ItemQuote.class);
			if (quote == null || quote.getId() == 0)
			{
				return null;
			}
			return quote;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable item from Runetick", e);
		}
		catch (IOException e)
		{
			if (e.getMessage() != null && e.getMessage().contains("HTTP 404"))
			{
				return null;
			}
			throw e;
		}
	}

	public Signals getSignals(int limit) throws IOException
	{
		String body = send("GET", "v1/signals/reversion?limit=" + Math.max(1, limit), null, false);
		try
		{
			Signals signals = gson.fromJson(body, Signals.class);
			if (signals == null)
			{
				return new Signals();
			}
			if (signals.getCheap() == null)
			{
				signals.setCheap(Collections.emptyList());
			}
			if (signals.getDear() == null)
			{
				signals.setDear(Collections.emptyList());
			}
			return signals;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable day-trade signals from Runetick", e);
		}
	}

	/**
	 * @param bought new bought count, or null to leave it alone
	 * @param sold new sold count, or null to leave it alone
	 */
	public void updatePlaced(String id, Integer bought, Integer sold) throws IOException
	{
		JsonObject body = new JsonObject();
		if (bought != null)
		{
			body.addProperty("bought", bought);
		}
		if (sold != null)
		{
			body.addProperty("sold", sold);
		}
		send("PATCH", "v1/me/placed/" + id, body, true);
	}

	public void logTrade(int itemId, String itemName, boolean buy, int qty, long price) throws IOException
	{
		JsonObject body = new JsonObject();
		body.addProperty("itemId", itemId);
		body.addProperty("itemName", itemName);
		body.addProperty("side", buy ? "buy" : "sell");
		body.addProperty("qty", qty);
		body.addProperty("price", price);
		send("POST", "v1/me/trades", body, true);
	}

	/**
	 * Push last-known cash + RuneLite GE history + live GE slots + net worth to the website.
	 *
	 * @param names itemId → display name (resolved on the client thread)
	 */
	public SyncResult syncLocal(long inventoryGp, long bankGp, boolean bankSeen,
		List<GeTrade> trades, Map<Integer, String> names,
		List<GeLiveOffer> geOffers, NetWorthSnap netWorth) throws IOException
	{
		JsonObject body = new JsonObject();
		body.addProperty("inventoryGp", Math.max(0, inventoryGp));
		body.addProperty("bankGp", Math.max(0, bankGp));
		body.addProperty("bankSeen", bankSeen);
		JsonArray arr = new JsonArray();
		if (trades != null)
		{
			for (GeTrade t : trades)
			{
				if (t == null || t.getItemId() <= 0 || t.getQuantity() <= 0 || t.getTime() <= 0)
				{
					continue;
				}
				JsonObject row = new JsonObject();
				row.addProperty("itemId", t.getItemId());
				String name = names == null ? null : names.get(t.getItemId());
				row.addProperty("itemName", name == null || name.isEmpty() ? ("Item " + t.getItemId()) : name);
				row.addProperty("side", t.isBuy() ? "buy" : "sell");
				row.addProperty("qty", t.getQuantity());
				row.addProperty("price", t.getPrice());
				row.addProperty("at", t.getTime());
				arr.add(row);
			}
		}
		body.add("trades", arr);

		JsonArray offers = new JsonArray();
		if (geOffers != null)
		{
			for (GeLiveOffer o : geOffers)
			{
				if (o == null || o.getItemId() <= 0)
				{
					continue;
				}
				JsonObject row = new JsonObject();
				row.addProperty("slot", o.getSlot());
				row.addProperty("itemId", o.getItemId());
				row.addProperty("itemName", o.getItemName() == null || o.getItemName().isEmpty()
					? ("Item " + o.getItemId()) : o.getItemName());
				row.addProperty("side", o.getSide() == null ? "buy" : o.getSide());
				row.addProperty("state", o.getState() == null ? "" : o.getState());
				row.addProperty("price", o.getPrice());
				row.addProperty("total", o.getTotal());
				row.addProperty("filled", o.getFilled());
				row.addProperty("spent", o.getSpent());
				offers.add(row);
			}
		}
		body.add("geOffers", offers);

		if (netWorth != null)
		{
			JsonObject nw = new JsonObject();
			nw.addProperty("cashGp", Math.max(0, netWorth.getCashGp()));
			nw.addProperty("inventoryCashGp", Math.max(0, netWorth.getInventoryCashGp()));
			nw.addProperty("invItemsGp", Math.max(0, netWorth.getInvItemsGp()));
			nw.addProperty("bankItemsGp", Math.max(0, netWorth.getBankItemsGp()));
			nw.addProperty("offersGp", Math.max(0, netWorth.getOffersGp()));
			nw.addProperty("invGeGp", Math.max(0, netWorth.getInvGeGp()));
			nw.addProperty("totalGp", Math.max(0, netWorth.getTotalGp()));
			nw.addProperty("bankItemsSeen", netWorth.isBankItemsSeen());
			nw.addProperty("sessionDeltaGp", netWorth.getSessionDeltaGp());
			nw.addProperty("sessionArmed", netWorth.isSessionArmed());
			nw.addProperty("at", netWorth.getAt() > 0 ? netWorth.getAt() : System.currentTimeMillis());
			body.add("netWorth", nw);
		}

		String text = send("POST", "v1/me/sync", body, true);
		try
		{
			SyncResult result = gson.fromJson(text, SyncResult.class);
			return result == null ? new SyncResult() : result;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Unreadable sync result from Runetick", e);
		}
	}

	static HttpUrl apiBase(String siteUrl)
	{
		String site = siteUrl == null || siteUrl.trim().isEmpty() ? RuneTickConfig.DEFAULT_SITE_URL : siteUrl.trim();
		HttpUrl url = HttpUrl.parse(site);
		return url == null ? null : url.newBuilder().addPathSegment("api").build();
	}

	private String send(String method, String pathAndQuery, JsonObject json, boolean requireAuth) throws IOException
	{
		HttpUrl base = apiBase(config.siteUrl());
		if (base == null)
		{
			throw new IOException("Invalid Runetick address: " + config.siteUrl());
		}

		String path = pathAndQuery;
		String query = null;
		int q = pathAndQuery.indexOf('?');
		if (q >= 0)
		{
			path = pathAndQuery.substring(0, q);
			query = pathAndQuery.substring(q + 1);
		}

		HttpUrl.Builder builder = base.newBuilder().addPathSegments(path);
		if (query != null)
		{
			HttpUrl parsed = HttpUrl.parse("http://local/?" + query);
			if (parsed != null)
			{
				for (int i = 0; i < parsed.querySize(); i++)
				{
					builder.addQueryParameter(parsed.queryParameterName(i), parsed.queryParameterValue(i));
				}
			}
		}
		HttpUrl url = builder.build();

		boolean withAuth = requireAuth;
		if (!requireAuth)
		{
			try
			{
				withAuth = session.isLinked();
			}
			catch (IOException e)
			{
				withAuth = false;
			}
		}

		for (int attempt = 0; ; attempt++)
		{
			Request.Builder req = new Request.Builder()
				.url(url)
				.header("Accept", "application/json")
				.method(method, json == null ? null : RequestBody.create(JSON, gson.toJson(json)));

			if (withAuth)
			{
				req.header("Authorization", "Bearer " + session.getIdToken());
			}

			try (Response response = httpClient.newCall(req.build()).execute())
			{
				ResponseBody body = response.body();
				String text = body == null ? "" : body.string();
				if (response.isSuccessful())
				{
					return text;
				}
				if (withAuth && response.code() == 401 && attempt == 0)
				{
					session.invalidateIdToken();
					continue;
				}
				throw new IOException("Runetick " + method + " /" + pathAndQuery + " failed: HTTP " + response.code() + " " + text);
			}
		}
	}
}
