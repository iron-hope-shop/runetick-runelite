package com.runetick;

/**
 * Heuristics for adverse market moves against an open GE offer.
 */
public final class OfferAlert
{
	/** Fire when market moves ≥ this fraction against the offer. */
	public static final double ADVERSE_PCT = 0.02;
	/** After-tax margin floor (gp/ea) before margin-crush alert. */
	public static final long MARGIN_FLOOR_GP = 50;

	public enum Kind
	{
		NONE,
		BUY_RAISE,
		SELL_LOWER,
		MARGIN_CRUSH
	}

	private OfferAlert()
	{
	}

	/**
	 * @param buy          true if this is a buy offer
	 * @param offerPrice   your bid (buy) or ask (sell)
	 * @param marketBuy    live wiki buy / high
	 * @param marketSell   live wiki sell / low
	 * @param afterTaxSell live after-tax sell (0 if unknown)
	 * @param costBasis    your buy price for margin check (0 to skip)
	 */
	public static Kind classify(boolean buy, long offerPrice, long marketBuy, long marketSell,
		long afterTaxSell, long costBasis)
	{
		if (offerPrice <= 0)
		{
			return Kind.NONE;
		}
		if (buy)
		{
			// market buy ≥ bid × 1.02
			if (marketBuy > 0 && marketBuy * 100L >= offerPrice * 102L)
			{
				return Kind.BUY_RAISE;
			}
		}
		else
		{
			// market sell ≤ ask × 0.98
			if (marketSell > 0 && marketSell * 100L <= offerPrice * 98L)
			{
				return Kind.SELL_LOWER;
			}
			if (costBasis > 0 && afterTaxSell > 0)
			{
				long margin = afterTaxSell - costBasis;
				if (margin < MARGIN_FLOOR_GP)
				{
					return Kind.MARGIN_CRUSH;
				}
			}
		}
		return Kind.NONE;
	}

	public static String fingerprint(int slot, int itemId, Kind kind)
	{
		return slot + ":" + itemId + ":" + (kind == null ? "NONE" : kind.name());
	}

	public static String message(Kind kind, String itemName)
	{
		String name = itemName == null || itemName.isEmpty() ? "offer" : itemName;
		switch (kind)
		{
			case BUY_RAISE:
				return name + ": market buy rose — raise bid or cancel";
			case SELL_LOWER:
				return name + ": market sell fell — lower ask or cancel";
			case MARGIN_CRUSH:
				return name + ": after-tax margin crushed — reconsider";
			default:
				return "";
		}
	}
}
