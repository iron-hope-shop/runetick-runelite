package com.runetick;

import java.util.function.ToLongFunction;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/**
 * What open Grand Exchange slots hold that is not yet in the bag or the bank.
 *
 * <p>While an offer is still running, the game lets you collect what has filled so far, and the
 * client cannot tell whether you have. Counting those fills here and again in the bag counted them
 * twice. So a running offer counts only what is still on the exchange: the coins locked on an
 * unfilled buy, or the items not yet sold. A finished or cancelled slot holds everything until you
 * collect it, so it counts all of it.
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

		long each = market > 0 ? market : offerPrice;
		switch (state)
		{
			case BUYING:
				return mul(remain, offerPrice);
			case SELLING:
				return mul(remain, each);
			case BOUGHT:
			case CANCELLED_BUY:
				return add(mul(filled, each), Math.max(0, mul(total, offerPrice) - spent));
			case SOLD:
			case CANCELLED_SELL:
				return add(mul(remain, each), spent);
			default:
				return 0;
		}
	}

	private static long mul(long a, long b)
	{
		if (a <= 0 || b <= 0)
		{
			return 0;
		}
		return a > Long.MAX_VALUE / 4 / b ? Long.MAX_VALUE / 4 : a * b;
	}

	private static long add(long a, long b)
	{
		long n = a + b;
		return n < 0 ? Long.MAX_VALUE / 4 : n;
	}
}
