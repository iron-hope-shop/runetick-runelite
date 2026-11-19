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
			}
		}
		qtyById = Collections.unmodifiableMap(next);
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
