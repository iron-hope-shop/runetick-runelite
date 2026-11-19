package com.runetick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls item quotes for live GE offers: ETA labels + adverse price desktop alerts (deduped).
 */
@Slf4j
@Singleton
public class OfferGuard
{
	private final RuneTickApi api;
	private final RuneTickConfig config;
	private final ErrorLog errorLog;

	/** slot -> last fired alert fingerprint still active. */
	private final Map<Integer, String> fired = new ConcurrentHashMap<>();
	/** itemId -> last quote volume (5m). */
	private final Map<Integer, Long> volumes = new ConcurrentHashMap<>();
	/** itemId -> last quote for GE tip / margin. */
	private final Map<Integer, RuneTickApi.ItemQuote> quotes = new ConcurrentHashMap<>();
	private volatile List<LiveRow> liveRows = Collections.emptyList();

	@Inject
	public OfferGuard(RuneTickApi api, RuneTickConfig config, ErrorLog errorLog)
	{
		this.api = api;
		this.config = config;
		this.errorLog = errorLog;
	}

	public List<LiveRow> getLiveRows()
	{
		return liveRows;
	}

	public long volumeOf(int itemId)
	{
		return volumes.getOrDefault(itemId, 0L);
	}

	public RuneTickApi.ItemQuote quoteOf(int itemId)
	{
		return quotes.get(itemId);
	}

	public String etaFor(int itemId, int remaining)
	{
		if (!config.offerEta())
		{
			return "";
		}
		return OfferEta.format(remaining, volumeOf(itemId));
	}

	/**
	 * Refresh quotes for the given live offers (call off the client thread).
	 * @param notifyDesktop receives alert messages to show
	 */
	public void poll(List<RuneTickApi.GeLiveOffer> offers, Consumer<String> notifyDesktop)
	{
		if (offers == null)
		{
			offers = Collections.emptyList();
		}
		Set<Integer> activeSlots = new HashSet<>();
		Set<Integer> needed = new HashSet<>();
		for (RuneTickApi.GeLiveOffer o : offers)
		{
			if (o == null || o.getItemId() <= 0)
			{
				continue;
			}
			activeSlots.add(o.getSlot());
			needed.add(o.getItemId());
		}
		fired.keySet().removeIf(slot -> !activeSlots.contains(slot));

		Map<Integer, RuneTickApi.ItemQuote> fetched = new HashMap<>();
		for (int itemId : needed)
		{
			try
			{
				RuneTickApi.ItemQuote q = api.getItem(itemId);
				if (q != null)
				{
					fetched.put(itemId, q);
					volumes.put(itemId, Math.max(0L, q.getVolume()));
					quotes.put(itemId, q);
				}
			}
			catch (Throwable t)
			{
				errorLog.log("OfferGuard quote failed for item " + itemId, t);
			}
		}
		volumes.keySet().removeIf(id -> !needed.contains(id));
		quotes.keySet().removeIf(id -> !needed.contains(id));

		List<LiveRow> rows = new ArrayList<>();
		boolean alertsOn = config.offerPriceAlerts() && config.desktopNotify();
		boolean etaOn = config.offerEta();

		for (RuneTickApi.GeLiveOffer o : offers)
		{
			if (o == null || o.getItemId() <= 0)
			{
				continue;
			}
			RuneTickApi.ItemQuote q = fetched.getOrDefault(o.getItemId(), quotes.get(o.getItemId()));
			long vol = q == null ? volumeOf(o.getItemId()) : Math.max(0L, q.getVolume());
			int remaining = Math.max(0, o.getTotal() - o.getFilled());
			String eta = etaOn ? OfferEta.format(remaining, vol) : "";

			boolean buy = !"sell".equalsIgnoreCase(o.getSide());
			long mktBuy = q == null ? 0 : q.getBuy();
			long mktSell = q == null ? 0 : q.getSell();
			long afterTax = q == null ? 0 : q.getAfterTaxSell();
			long costBasis = 0;
			if (!buy)
			{
				// Best-effort: use spent/filled as unit cost when available.
				if (o.getFilled() > 0 && o.getSpent() > 0)
				{
					costBasis = o.getSpent() / o.getFilled();
				}
			}
			OfferAlert.Kind kind = OfferAlert.classify(buy, o.getPrice(), mktBuy, mktSell, afterTax, costBasis);
			String fp = OfferAlert.fingerprint(o.getSlot(), o.getItemId(), kind);
			if (alertsOn && kind != OfferAlert.Kind.NONE)
			{
				String prev = fired.get(o.getSlot());
				if (!fp.equals(prev))
				{
					fired.put(o.getSlot(), fp);
					String msg = OfferAlert.message(kind, o.getItemName());
					if (msg != null && !msg.isEmpty() && notifyDesktop != null)
					{
						try
						{
							notifyDesktop.accept(msg);
						}
						catch (Throwable t)
						{
							errorLog.log("OfferGuard notify failed", t);
						}
					}
				}
			}
			else if (kind == OfferAlert.Kind.NONE)
			{
				fired.remove(o.getSlot());
			}

			rows.add(new LiveRow(
				o.getSlot(),
				o.getItemId(),
				o.getItemName(),
				buy ? "buy" : "sell",
				o.getPrice(),
				o.getFilled(),
				o.getTotal(),
				eta,
				kind));
		}
		liveRows = Collections.unmodifiableList(rows);
	}

	public void clear()
	{
		fired.clear();
		volumes.clear();
		quotes.clear();
		liveRows = Collections.emptyList();
	}

	public static final class LiveRow
	{
		public final int slot;
		public final int itemId;
		public final String name;
		public final String side;
		public final long price;
		public final int filled;
		public final int total;
		public final String eta;
		public final OfferAlert.Kind alert;

		LiveRow(int slot, int itemId, String name, String side, long price, int filled, int total,
			String eta, OfferAlert.Kind alert)
		{
			this.slot = slot;
			this.itemId = itemId;
			this.name = name == null ? ("#" + itemId) : name;
			this.side = side;
			this.price = price;
			this.filled = filled;
			this.total = total;
			this.eta = eta == null ? "" : eta;
			this.alert = alert == null ? OfferAlert.Kind.NONE : alert;
		}
	}
}
