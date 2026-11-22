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

	/** Call on the client thread while the bank container is available. */
	public void refresh(ItemContainer bank)
	{
		if (bank == null)
		{
			return;
		}
		Map<Integer, Integer> next = new HashMap<>();
		Map<Integer, Integer> raws = new HashMap<>();
		Item[] items = bank.getItems();
		if (items != null)
		{
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
				raws.put(raw, id);
			}
		}
		qtyById = Collections.unmodifiableMap(next);
		canonByRaw = Collections.unmodifiableMap(raws);
		seen = true;
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
