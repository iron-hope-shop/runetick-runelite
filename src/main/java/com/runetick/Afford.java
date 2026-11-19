package com.runetick;

/**
 * How many units you can buy with known cash at a given price, capped by GE limit when set.
 */
final class Afford
{
	private Afford()
	{
	}

	/**
	 * @param cashGp inv + last-known bank (or inv only if bank never opened)
	 * @param buyPrice gp per item
	 * @param limit GE 4h buy limit, or 0 if unknown
	 */
	static long units(long cashGp, long buyPrice, int limit)
	{
		if (buyPrice <= 0 || cashGp <= 0)
		{
			return 0;
		}
		long afford = cashGp / buyPrice;
		if (limit > 0)
		{
			return Math.min(afford, limit);
		}
		return afford;
	}
}
