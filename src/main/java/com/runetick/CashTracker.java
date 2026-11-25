package com.runetick;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;

/**
 * Inventory cash is live while logged in. Bank cash is only readable while the bank is open —
 * we keep the last seen bank total in memory and on disk so it survives logout/restart.
 */
@Slf4j
@Singleton
public class CashTracker
{
	private static final int COINS = ItemID.COINS_995;
	private static final int PLATINUM = ItemID.PLATINUM_TOKEN;
	private static final long PLATINUM_VALUE = 1_000L;

	private final Client client;
	private final CashStore store;

	private volatile long inventoryGp;
	private volatile long bankGp;
	private volatile boolean bankSeen;
	private volatile long savedAt;
	private volatile boolean dirty;

	@Inject
	public CashTracker(Client client, CashStore store)
	{
		this.client = client;
		this.store = store;
		restore();
	}

	public long getInventoryGp()
	{
		return inventoryGp;
	}

	public long getBankGp()
	{
		return bankGp;
	}

	public boolean isBankSeen()
	{
		return bankSeen;
	}

	public long getSavedAt()
	{
		return savedAt;
	}

	/** Inventory + last-seen bank (0 bank until opened once, then persists). Bank is left out when the player says so. */
	public long getTotalKnownGp()
	{
		return inventoryGp + (bankSeen && countBank ? bankGp : 0);
	}

	private volatile boolean countBank = true;
	private volatile long account = Long.MIN_VALUE;

	public void setCountBank(boolean countBank)
	{
		this.countBank = countBank;
	}

	public boolean isCountBank()
	{
		return countBank;
	}

	/** Switch to this character's last-known cash. Returns true when the character changed. */
	public boolean useAccount(long accountHash)
	{
		if (accountHash == account)
		{
			return false;
		}
		persist();
		account = accountHash;
		store.useAccount(accountHash);
		inventoryGp = 0;
		bankGp = 0;
		bankSeen = false;
		savedAt = 0;
		dirty = false;
		restore();
		return true;
	}

	public void restore()
	{
		CashStore.Snapshot snap = store.load();
		if (snap == null)
		{
			return;
		}
		inventoryGp = snap.getInventoryGp();
		bankGp = snap.getBankGp();
		bankSeen = snap.isBankSeen();
		savedAt = snap.getSavedAt();
		dirty = false;
	}

	public void refreshInventory()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		// A missing container is not an empty bag. Writing 0 here stuck wealth at zero.
		if (inv == null)
		{
			return;
		}
		long next = countGp(inv);
		if (next != inventoryGp)
		{
			inventoryGp = next;
			dirty = true;
			persist();
		}
	}

	/** Coins that left the bag at a deposit box, once the bank has been opened. */
	public void applyCoinDeposit(long removed)
	{
		if (!countBank || !bankSeen || removed <= 0)
		{
			return;
		}
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		long live = bank == null ? -1 : countGp(bank);
		long miss = DepositToBank.stillMissing(bankGp, live, removed);
		if (miss == 0 && bank != null)
		{
			refreshBank();
			return;
		}
		if (miss <= 0)
		{
			return;
		}
		long next = bankGp + miss;
		bankGp = next < bankGp ? Long.MAX_VALUE / 4 : next;
		dirty = true;
		persist();
	}

	public void refreshBank()
	{
		if (!countBank)
		{
			return;
		}
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null)
		{
			return;
		}
		long next = countGp(bank);
		if (!bankSeen || next != bankGp)
		{
			bankGp = next;
			bankSeen = true;
			dirty = true;
			persist();
		}
	}

	/**
	 * Leave last-known cash alone on logout — only clear the live inventory read so we do not
	 * pretend the bag is still open. Bank cache stays until the next bank open refreshes it.
	 */
	public void onLogout()
	{
		// Keep inventoryGp as last known for panel/site until the next live refresh.
		persist();
	}

	public void persist()
	{
		if (!dirty && savedAt > 0)
		{
			return;
		}
		try
		{
			store.save(inventoryGp, bankGp, bankSeen);
			savedAt = System.currentTimeMillis();
			dirty = false;
		}
		catch (IOException e)
		{
			log.error("Could not save last-known cash", e);
		}
	}

	static long countGp(ItemContainer container)
	{
		if (container == null)
		{
			return 0;
		}
		long coins = container.count(COINS);
		long platinum = container.count(PLATINUM);
		return coins + platinum * PLATINUM_VALUE;
	}
}
