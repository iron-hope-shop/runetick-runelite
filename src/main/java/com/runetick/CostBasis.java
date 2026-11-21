package com.runetick;

import java.util.ArrayDeque;
import java.util.List;

/**
 * What is still unsold from Grand Exchange history. Buys add lots, later sells
 * consume the oldest lots. The remainder is what a current sell was bought for.
 */
public final class CostBasis
{
	public static final class Open
	{
		public final int qty;
		public final long each;

		Open(int qty, long each)
		{
			this.qty = qty;
			this.each = each;
		}
	}

	private static final class Lot
	{
		int qty;
		final long each;

		Lot(int qty, long each)
		{
			this.qty = qty;
			this.each = each;
		}
	}

	private CostBasis()
	{
	}

	/** Weighted price of buys for {@code itemId} that history has not sold yet. Null if none remain. */
	public static Open openBuys(List<GeTrade> trades, int itemId)
	{
		if (trades == null || trades.isEmpty() || itemId <= 0)
		{
			return null;
		}
		ArrayDeque<Lot> lots = new ArrayDeque<>();
		for (GeTrade trade : trades)
		{
			if (trade == null || trade.getItemId() != itemId || trade.getQuantity() <= 0 || trade.getPrice() <= 0)
			{
				continue;
			}
			if (trade.isBuy())
			{
				lots.addLast(new Lot(trade.getQuantity(), trade.getPrice()));
				continue;
			}
			int left = trade.getQuantity();
			while (left > 0 && !lots.isEmpty())
			{
				Lot lot = lots.peekFirst();
				int take = Math.min(left, lot.qty);
				lot.qty -= take;
				left -= take;
				if (lot.qty == 0)
				{
					lots.removeFirst();
				}
			}
		}
		long cost = 0;
		int qty = 0;
		for (Lot lot : lots)
		{
			cost += lot.each * (long) lot.qty;
			qty += lot.qty;
		}
		if (qty <= 0)
		{
			return null;
		}
		return new Open(qty, cost / qty);
	}
}
