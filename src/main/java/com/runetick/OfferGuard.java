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

	/** slot -> last fired alert fingerprint still active. */
	private final Map<Integer, String> fired = new ConcurrentHashMap<>();
	/** itemId -> last quote volume (5m). */
	private final Map<Integer, Long> volumes = new ConcurrentHashMap<>();
	/** itemId -> last quote for GE tip / margin. */
	private final Map<Integer, RuneTickApi.ItemQuote> quotes = new ConcurrentHashMap<>();
	private volatile List<LiveRow> liveRows = Collections.emptyList();

	@Inject
	public OfferGuard(RuneTickApi api, RuneTickConfig config)
	{
		this.api = api;
		this.config = config;
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
		return etaFor(itemId, remaining, true, 0);
	}

	public String etaFor(int itemId, int remaining, boolean buy, long offerPrice)
	{
		if (!config.offerEta())
		{
			return "";
		}
		OfferEta.Input in = OfferEta.fromQuote(remaining, buy, offerPrice, quotes.get(itemId));
		if (in.volume5m <= 0 && volumes.containsKey(itemId))
		{
			in.volume5m = volumes.get(itemId);
		}
		String eta = OfferEta.format(in);
		if (offerPrice > 0 && in.marketBuy > 0 && in.marketSell > 0)
		{
			OfferAlert.Kind queue = buy
				? (in.marketBuy >= offerPrice * 1.02 ? OfferAlert.Kind.BUY_RAISE : OfferAlert.Kind.NONE)
				: (offerPrice > in.marketSell * 1.02 ? OfferAlert.Kind.SELL_LOWER : OfferAlert.Kind.NONE);
			return OfferEta.paceLabel(queue, eta);
		}
		return eta;
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
				log.error("OfferGuard quote failed for item " + itemId, t);
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
			int remaining = Math.max(0, o.getTotal() - o.getFilled());

			boolean buy = !"sell".equalsIgnoreCase(o.getSide());
			String eta = "";
			if (etaOn)
			{
				OfferEta.Input in = OfferEta.fromQuote(remaining, buy, o.getPrice(), q);
				eta = OfferEta.format(in);
			}

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
			int pct = config.offerAlertPercent();
			if (pct < 1) pct = 2;
			long floor = config.offerMarginFloor();
			OfferAlert.Kind kind = OfferAlert.classify(buy, o.getPrice(), mktBuy, mktSell, afterTax, costBasis, pct, floor);
			if (kind == OfferAlert.Kind.BUY_RAISE && !config.alertBuyRaise()) kind = OfferAlert.Kind.NONE;
			if (kind == OfferAlert.Kind.SELL_LOWER && !config.alertSellLower()) kind = OfferAlert.Kind.NONE;
			if (kind == OfferAlert.Kind.MARGIN_CRUSH && !config.alertMarginCrush()) kind = OfferAlert.Kind.NONE;
			if (etaOn)
			{
				eta = OfferEta.paceLabel(kind, eta);
			}
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
							log.error("OfferGuard notify failed", t);
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