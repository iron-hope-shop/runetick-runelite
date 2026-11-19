package com.runetick;

import java.util.function.ToLongFunction;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/**
 * Mark-to-market value of open / collectable GE offers.
 *
 * <p>Buy: unfilled remainder is still your locked cash at the offer price; filled units waiting
 * to collect are items at GE price.
 *
 * <p>Sell: unsold remainder is items at GE price; {@code spent} is coins waiting to collect.
 */
final class OfferValue
{
	private OfferValue()
	{
	}

	static long estimate(GrandExchangeOffer[] offers, ToLongFunction<Integer> marketPriceOf)
	{
		if (offers == null || offers.length == 0)
		{
			return 0;
		}
		long sum = 0;
		for (GrandExchangeOffer offer : offers)
		{
			long v = one(offer, marketPriceOf);
			long next = sum + v;
			if (next < sum)
			{
				return Long.MAX_VALUE / 4;
			}
			sum = next;
		}
		return sum;
	}

	static long one(GrandExchangeOffer offer, ToLongFunction<Integer> marketPriceOf)
	{
		if (offer == null)
		{
			return 0;
		}
		GrandExchangeOfferState state = offer.getState();
		if (state == null || state == GrandExchangeOfferState.EMPTY)
		{
			return 0;
		}
		int itemId = offer.getItemId();
		int total = Math.max(0, offer.getTotalQuantity());
		int filled = Math.max(0, Math.min(total, offer.getQuantitySold()));
		int remain = Math.max(0, total - filled);
		long offerPrice = Math.max(0, offer.getPrice());
		long spent = Math.max(0, offer.getSpent());
		long market = 0;
		if (itemId > 0 && marketPriceOf != null)
		{
			market = Math.max(0, marketPriceOf.applyAsLong(itemId));
		}

		boolean buy = state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;

		if (buy)
		{
			// Locked cash still sitting on the offer + filled items not yet collected.
			long lockedCash = remain * offerPrice;
			long itemsWaiting = filled * (market > 0 ? market : offerPrice);
			return add(lockedCash, itemsWaiting);
		}

		// Sell / cancelled sell: unsold stock + coins from fills waiting to collect.
		long itemsWaiting = remain * (market > 0 ? market : offerPrice);
		return add(itemsWaiting, spent);
	}

	private static long add(long a, long b)
	{
		long n = a + b;
		return n < 0 ? Long.MAX_VALUE / 4 : n;
	}
}
