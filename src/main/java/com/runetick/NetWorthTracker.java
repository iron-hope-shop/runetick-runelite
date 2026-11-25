package com.runetick;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.client.game.ItemManager;

/**
 * Estimates net worth: known cash + GE prices for bag, worn and bank items + what open GE offers hold.
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
	private final WornHoldings wornHoldings;

	private volatile long invItemsGp;
	private volatile long wornGp;
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
		BankHoldings bankHoldings,
		WornHoldings wornHoldings)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.cashTracker = cashTracker;
		this.inventoryHoldings = inventoryHoldings;
		this.bankHoldings = bankHoldings;
		this.wornHoldings = wornHoldings;
	}

	/** Reprice holdings + GE offers. Call on the client thread. */
	public void revalue()
	{
		invItemsGp = ItemValue.estimate(inventoryHoldings.snapshot(), this::price);
		wornGp = ItemValue.estimate(wornHoldings.snapshot(), this::price);
		bankItemsGp = bankHoldings.isSeen() ? ItemValue.estimate(bankHoldings.snapshot(), this::price) : 0;
		offersGp = OfferValue.estimate(safeOffers(), this::price);
		armSessionIfReady();
	}

	public void onLogout()
	{
		sessionBaseline = null;
		sessionArmed = false;
		offersGp = 0;
		invItemsGp = 0;
		wornGp = 0;
		// Keep last bankItemsGp — bank scan persists across logout like CashTracker.
	}

	/** A different character logged in: nothing from the last one carries over. */
	public void onAccountChange()
	{
		onLogout();
		bankItemsGp = 0;
	}

	/** Start the session clock again from the current total (after a website journal reset, or when the bank is put in or left out). */
	public void rebaseSession()
	{
		sessionBaseline = null;
		sessionArmed = false;
		armSessionIfReady();
	}

	public NetWorth snapshot()
	{
		long total = totalNow();
		long delta = sessionArmed && sessionBaseline != null ? total - sessionBaseline : 0;
		return new NetWorth(
			cashTracker.getTotalKnownGp(),
			cashTracker.getInventoryGp(),
			invItemsGp,
			wornGp,
			bankItemsGp,
			offersGp,
			bankHoldings.isSeen(),
			cashTracker.isCountBank(),
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
		if (!SessionDelta.ready(inventoryHoldings.isSeen(), bankHoldings.isSeen(), cashTracker.isCountBank()))
		{
			return;
		}
		sessionBaseline = totalNow();
		sessionArmed = true;
	}

	private long totalNow()
	{
		return new NetWorth(cashTracker.getTotalKnownGp(), cashTracker.getInventoryGp(), invItemsGp, wornGp, bankItemsGp, offersGp,
			bankHoldings.isSeen(), cashTracker.isCountBank(), 0, false).totalGp;
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
