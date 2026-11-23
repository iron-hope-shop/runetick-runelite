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
		return classify(buy, offerPrice, marketBuy, marketSell, afterTaxSell, costBasis, 2, MARGIN_FLOOR_GP);
	}

	/**
	 * @param adversePct  how far the market must move against the offer, 1 to 25
	 * @param marginFloor after-tax gold per item below which a sell is called crushed
	 */
	public static Kind classify(boolean buy, long offerPrice, long marketBuy, long marketSell,
		long afterTaxSell, long costBasis, int adversePct, long marginFloor)
	{
		if (offerPrice <= 0)
		{
			return Kind.NONE;
		}
		int pct = Math.max(1, Math.min(25, adversePct));
		long move = Math.max(1L, offerPrice * pct / 100L);
		if (buy)
		{
			if (marketBuy > 0 && marketBuy >= offerPrice + move)
			{
				return Kind.BUY_RAISE;
			}
		}
		else
		{
			if (marketSell > 0 && marketSell <= offerPrice - move)
			{
				return Kind.SELL_LOWER;
			}
			if (costBasis > 0 && afterTaxSell > 0 && afterTaxSell - costBasis < marginFloor)
			{
				return Kind.MARGIN_CRUSH;
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
