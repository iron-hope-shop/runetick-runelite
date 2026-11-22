package com.runetick;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.client.game.ItemManager;

/**
 * Last-seen bank item quantities (canonical tradeable ids). Updated while the bank is open;
 * kept after close so the board can tag fat/dip/flip hits you already hold.
 */
@Singleton
public class BankHoldings
{
	private static final int COINS = ItemID.COINS_995;
	private static final int PLATINUM = ItemID.PLATINUM_TOKEN;

	private final ItemManager itemManager;

	private volatile Map<Integer, Integer> qtyById = Collections.emptyMap();
	private volatile Map<Integer, Integer> canonByRaw = Collections.emptyMap();
	private volatile boolean seen;

	@Inject
	public BankHoldings(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public boolean isSeen()
	{
		return seen;
	}

	public int qty(int itemId)
	{
		if (itemId <= 0)
		{
			return 0;
		}
		Integer q = qtyById.get(itemId);
		return q == null ? 0 : q;
	}

	/** The tradeable id for an id as it appears in the container. Zero if it was not there on the last read. */
	public int canonicalOf(int rawId)
	{
		Integer id = canonByRaw.get(rawId);
		return id == null ? 0 : id;
	}

	public Map<Integer, Integer> snapshot()
	{
		return qtyById;
	}

	public void clear()
	{
		qtyById = Collections.emptyMap();
		seen = false;
	}

	/**
	 * Items that left the bag at a deposit box. Merges into the last full bank scan.
	 * If the bank container already contains them, that container replaces the cache.
	 */
	public void applyDeposit(Map<Integer, Integer> removed, ItemContainer bank)
	{
		if (!seen || removed == null || removed.isEmpty())
		{
			return;
		}
		Map<Integer, Integer> live = bank == null ? null : quantities(bank);
		Map<Integer, Integer> add = new HashMap<>();
		boolean containerHasAll = live != null;
		for (Map.Entry<Integer, Integer> e : removed.entrySet())
		{
			int id = e.getKey();
			int qty = e.getValue() == null ? 0 : e.getValue();
			if (id <= 0 || qty <= 0 || id == COINS || id == PLATINUM)
			{
				continue;
			}
			long liveQty = live == null ? -1 : live.getOrDefault(id, 0);
			long miss = DepositToBank.stillMissing(qty(id), liveQty, qty);
			if (miss > 0)
			{
				containerHasAll = false;
				add.put(id, miss > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) miss);
			}
		}
		if (containerHasAll)
		{
			refresh(bank);
			return;
		}
		if (add.isEmpty())
		{
			return;
		}
		Map<Integer, Integer> next = new HashMap<>(qtyById);
		for (Map.Entry<Integer, Integer> e : add.entrySet())
		{
			next.merge(e.getKey(), e.getValue(), Integer::sum);
		}
		qtyById = Collections.unmodifiableMap(next);
	}

	/** Call on the client thread while the bank container is available. */
	public void refresh(ItemContainer bank)
	{
		if (bank == null)
		{
			return;
		}
		Map<Integer, Integer> next = quantities(bank);
		Map<Integer, Integer> raws = rawCanonical(bank);
		qtyById = Collections.unmodifiableMap(next);
		canonByRaw = Collections.unmodifiableMap(raws);
		seen = true;
	}

	private Map<Integer, Integer> rawCanonical(ItemContainer bank)
	{
		Map<Integer, Integer> raws = new HashMap<>();
		Item[] items = bank.getItems();
		if (items == null)
		{
			return raws;
		}
		for (Item item : items)
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			int raw = item.getId();
			if (raw == COINS || raw == PLATINUM)
			{
				continue;
			}
			int id = canonicalize(raw);
			if (id > 0 && id != COINS && id != PLATINUM)
			{
				raws.put(raw, id);
			}
		}
		return raws;
	}

	private Map<Integer, Integer> quantities(ItemContainer bank)
	{
		Map<Integer, Integer> next = new HashMap<>();
		Item[] items = bank.getItems();
		if (items == null)
		{
			return next;
		}
		for (Item item : items)
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			int raw = item.getId();
			if (raw == COINS || raw == PLATINUM)
			{
				continue;
			}
			int id = canonicalize(raw);
			if (id <= 0 || id == COINS || id == PLATINUM)
			{
				continue;
			}
			next.merge(id, item.getQuantity(), Integer::sum);
		}
		return next;
	}

	private int canonicalize(int itemId)
	{
		try
		{
			return itemManager.canonicalize(itemId);
		}
		catch (Throwable t)
		{
			return itemId;
		}
	}
}
