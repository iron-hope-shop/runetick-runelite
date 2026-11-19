package com.runetick;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.client.game.ItemManager;

/**
 * Estimates net worth: known cash + GE prices for inventory/bank items + open GE offers.
 *
 * <p>Session delta only starts after inventory <em>and</em> bank have been scanned, so discovering
 * bank wealth mid-login is not counted as profit. GE offer moves after that still count.
 */
@Singleton
public class NetWorthTracker
{
	private final Client client;
	private final ItemManager itemManager;
	private final CashTracker cashTracker;
	private final InventoryHoldings inventoryHoldings;
	private final BankHoldings bankHoldings;

	private volatile long invItemsGp;
	private volatile long bankItemsGp;
	private volatile long offersGp;
	private volatile Long sessionBaseline;
	private volatile boolean sessionArmed;

	@Inject
	public NetWorthTracker(
		Client client,
		ItemManager itemManager,
		CashTracker cashTracker,
		InventoryHoldings inventoryHoldings,
		BankHoldings bankHoldings)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.cashTracker = cashTracker;
		this.inventoryHoldings = inventoryHoldings;
		this.bankHoldings = bankHoldings;
	}

	/** Reprice holdings + GE offers. Call on the client thread. */
	public void revalue()
	{
		invItemsGp = ItemValue.estimate(inventoryHoldings.snapshot(), this::price);
		if (bankHoldings.isSeen())
		{
			bankItemsGp = ItemValue.estimate(bankHoldings.snapshot(), this::price);
		}
		offersGp = OfferValue.estimate(safeOffers(), this::price);
		armSessionIfReady();
	}

	public void onLogout()
	{
		sessionBaseline = null;
		sessionArmed = false;
		offersGp = 0;
		invItemsGp = 0;
		// Keep last bankItemsGp — bank scan persists across logout like CashTracker.
	}

	public NetWorth snapshot()
	{
		long total = totalNow();
		long delta = sessionArmed && sessionBaseline != null ? total - sessionBaseline : 0;
		return new NetWorth(
			cashTracker.getTotalKnownGp(),
			cashTracker.getInventoryGp(),
			invItemsGp,
			bankItemsGp,
			offersGp,
			bankHoldings.isSeen(),
			delta,
			sessionArmed);
	}

	boolean isSessionArmed()
	{
		return sessionArmed;
	}

	Long getSessionBaseline()
	{
		return sessionBaseline;
	}

	private void armSessionIfReady()
	{
		if (sessionArmed)
		{
			return;
		}
		// Need a full picture: bag items scanned + bank opened at least once this install/session.
		if (!SessionDelta.ready(inventoryHoldings.isSeen(), bankHoldings.isSeen()))
		{
			return;
		}
		sessionBaseline = totalNow();
		sessionArmed = true;
	}

	private long totalNow()
	{
		long cash = cashTracker.getTotalKnownGp();
		long items = invItemsGp + (bankHoldings.isSeen() ? bankItemsGp : 0);
		long total = cash + items + offersGp;
		return total < 0 ? Long.MAX_VALUE / 4 : total;
	}

	private GrandExchangeOffer[] safeOffers()
	{
		try
		{
			return client.getGrandExchangeOffers();
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	private long price(int itemId)
	{
		try
		{
			return Math.max(0, itemManager.getItemPrice(itemId));
		}
		catch (Throwable t)
		{
			return 0;
		}
	}
}
