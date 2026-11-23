package com.runetick;

/**
 * Fill-time estimate aligned with desk gp/h (volume share + 4h limit pace),
 * adjusted for queue position and round-fill probability.
 */
public final class OfferEta
{
	private static final double VOLUME_SHARE = 0.1;
	private static final double MOVE = 1.02;

	private OfferEta()
	{
	}

	public static final class Input
	{
		public int remaining;
		public long volume5m;
		public int limit = 1;
		public Double pFill;
		public boolean buy;
		public long offerPrice;
		public long marketBuy;
		public long marketSell;
	}

	/** Legacy wiki pace (volume5m × 12). */
	public static long ratePerHour(long volume5m)
	{
		if (volume5m <= 0)
		{
			return 0;
		}
		return volume5m * 12L;
	}

	public static double deskUnitsPerHour(long volume5m, int limit)
	{
		int lim = Math.max(1, limit);
		double limitPace = lim / 4.0;
		double volumePace = VOLUME_SHARE * Math.max(0, volume5m) * 12.0;
		return Math.max(0, Math.min(limitPace, volumePace));
	}

	public static double queueCompetitiveness(boolean buy, long offerPrice, long marketBuy, long marketSell)
	{
		if (offerPrice <= 0 || marketBuy <= 0 || marketSell <= 0)
		{
			return 0;
		}
		if (buy)
		{
			if (offerPrice >= marketSell)
			{
				return 0;
			}
			if (marketBuy >= offerPrice * MOVE)
			{
				return 0.05;
			}
			if (offerPrice >= marketBuy)
			{
				return 1;
			}
			double ratio = (double) offerPrice / marketBuy;
			return Math.max(0.08, Math.min(1, ratio * ratio));
		}
		if (offerPrice <= marketBuy)
		{
			return 1;
		}
		if (offerPrice > marketSell * MOVE)
		{
			return 0.05;
		}
		if (offerPrice <= marketSell)
		{
			return 1;
		}
		double ratio = (double) marketSell / offerPrice;
		return Math.max(0.08, Math.min(1, ratio * ratio));
	}

	public static double effectiveUnitsPerHour(Input in)
	{
		if (in == null)
		{
			return 0;
		}
		double rate = deskUnitsPerHour(in.volume5m, in.limit);
		if (in.offerPrice > 0 && in.marketBuy > 0 && in.marketSell > 0)
		{
			rate *= queueCompetitiveness(in.buy, in.offerPrice, in.marketBuy, in.marketSell);
		}
		if (in.pFill != null && !in.pFill.isNaN())
		{
			double stall = Math.max(0.12, Math.min(1, in.pFill));
			rate *= stall;
		}
		return rate;
	}

	public static long etaMinutes(Input in)
	{
		if (in == null || in.remaining <= 0)
		{
			return 0;
		}
		double rate = effectiveUnitsPerHour(in);
		if (rate <= 0)
		{
			return Long.MAX_VALUE;
		}
		return Math.max(1L, (long) Math.ceil(in.remaining * 60.0 / rate));
	}

	public static long etaMinutes(int remaining, long volume5m)
	{
		Input in = new Input();
		in.remaining = remaining;
		in.volume5m = volume5m;
		in.limit = 1;
		return etaMinutes(in);
	}

	public static String format(Input in)
	{
		if (in == null || in.remaining <= 0)
		{
			return "done";
		}
		long mins = etaMinutes(in);
		return formatMinutes(mins);
	}

	public static String format(int remaining, long volume5m)
	{
		Input in = new Input();
		in.remaining = remaining;
		in.volume5m = volume5m;
		in.limit = 1;
		return format(in);
	}

	static String formatMinutes(long mins)
	{
		if (mins == Long.MAX_VALUE || mins > 24L * 60L)
		{
			return "slow";
		}
		if (mins < 60)
		{
			return "~" + mins + "m";
		}
		long hours = mins / 60;
		if (hours < 48)
		{
			return "~" + hours + "h";
		}
		return "slow";
	}

	static String paceLabel(OfferAlert.Kind alert, String eta)
	{
		if (eta == null || eta.isEmpty() || "done".equals(eta))
		{
			return "";
		}
		if (alert == OfferAlert.Kind.BUY_RAISE || alert == OfferAlert.Kind.SELL_LOWER)
		{
			return "not filling";
		}
		return eta;
	}

	static Input fromQuote(int remaining, boolean buy, long offerPrice, RuneTickApi.ItemQuote q)
	{
		Input in = new Input();
		in.remaining = remaining;
		in.buy = buy;
		in.offerPrice = offerPrice;
		if (q == null)
		{
			return in;
		}
		in.volume5m = Math.max(0L, q.getVolume());
		in.limit = Math.max(1, q.getLimit());
		in.marketBuy = Math.max(0L, q.getBuy());
		in.marketSell = Math.max(0L, q.getSell());
		RuneTickApi.Analytics a = q.getAnalytics();
		if (a != null)
		{
			in.pFill = a.getFillProb();
		}
		return in;
	}
}