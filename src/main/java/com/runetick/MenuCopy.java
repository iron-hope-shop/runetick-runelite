package com.runetick;

/** The chat lines the right-click options print. */
final class MenuCopy
{
	private MenuCopy()
	{
	}

	static String priceCheck(String name, RuneTickApi.ItemQuote q)
	{
		if (q == null || (q.getBuy() <= 0 && q.getSell() <= 0))
		{
			return label(name) + ": no recent trades.";
		}
		StringBuilder s = new StringBuilder(label(name)).append(": buy ").append(PriceFormat.gp(q.getBuy()))
			.append(" · sell ").append(PriceFormat.gp(q.getSell()));
		long each = FocusCopy.eachProfit(q.getBuy(), q.getSell());
		if (each != Long.MIN_VALUE)
		{
			s.append(" · ").append(signed(each)).append(" each after tax");
		}
		RuneTickApi.Analytics a = q.getAnalytics();
		int odds = a == null ? -1 : FocusCopy.in10(a.getFillProb());
		if (odds >= 0)
		{
			s.append(" · fills ").append(odds).append(" in 10");
		}
		String today = a == null ? "" : FocusCopy.priceToday(a.getZScore24h());
		if (!today.isEmpty())
		{
			s.append(" · ").append(today.toLowerCase());
		}
		if (q.getLimit() > 0)
		{
			s.append(" · limit ").append(PriceFormat.gp(q.getLimit()));
		}
		return s.toString();
	}

	static String sellValue(String name, long qty, RuneTickApi.ItemQuote q)
	{
		if (qty <= 0)
		{
			return "You have no " + label(name) + " in your bag or bank.";
		}
		String head = PriceFormat.gp(qty) + " × " + label(name);
		if (q == null || q.getSell() <= 0)
		{
			return head + ": no recent trades.";
		}
		long keepEach = q.getAfterTaxSell() > 0 ? q.getAfterTaxSell() : BankView.afterTax(q.getSell());
		StringBuilder s = new StringBuilder(head).append(": sells for about ")
			.append(BankView.compact(keepEach * qty)).append(" after tax (")
			.append(PriceFormat.gp(keepEach)).append(" each)");
		Integer alch = q.getHighalch();
		if (alch != null && alch > 0)
		{
			long alchTotal = alch * qty;
			s.append(". High alch ").append(BankView.compact(alchTotal));
			if (alch > keepEach)
			{
				s.append(", better than selling");
			}
		}
		if (q.getLimit() > 0 && qty > q.getLimit() * 4L)
		{
			s.append(". Selling this much will push the price down");
		}
		return s.append('.').toString();
	}

	static String slotCall(String name, boolean buy, long offerPrice, int filled, int total,
		RuneTickApi.ItemQuote q, OfferAlert.Kind kind)
	{
		String item = label(name);
		String progress = total > 0 ? " (" + PriceFormat.gp(filled) + "/" + PriceFormat.gp(total) + " done)" : "";
		if (q == null || offerPrice <= 0)
		{
			return item + progress + ": no live price to judge this offer by yet.";
		}
		long keep = q.getAfterTaxSell() > 0 ? q.getAfterTaxSell() : BankView.afterTax(q.getSell());
		if (kind == OfferAlert.Kind.BUY_RAISE)
		{
			return "± Reprice " + item + progress + ": people are now paying " + PriceFormat.gp(q.getBuy())
				+ ", " + pct(q.getBuy(), offerPrice) + " over your " + PriceFormat.gp(offerPrice)
				+ ". Raise toward " + PriceFormat.gp(q.getBuy()) + " or it may not fill"
				+ marginTail(keep - q.getBuy()) + ".";
		}
		if (kind == OfferAlert.Kind.SELL_LOWER)
		{
			return "± Reprice " + item + progress + ": people are now selling at " + PriceFormat.gp(q.getSell())
				+ ", " + pct(offerPrice, q.getSell()) + " under your " + PriceFormat.gp(offerPrice)
				+ ". Lower toward " + PriceFormat.gp(q.getSell()) + " to sell; you keep "
				+ PriceFormat.gp(keep) + " each.";
		}
		if (kind == OfferAlert.Kind.MARGIN_CRUSH)
		{
			return "✕ Pull " + item + progress + ": after tax there is almost nothing left at "
				+ PriceFormat.gp(offerPrice) + ". Cancel it, or hold the items until the price recovers.";
		}
		if (buy)
		{
			return "✓ Hold " + item + progress + ": your " + PriceFormat.gp(offerPrice)
				+ " is in line with the market (" + PriceFormat.gp(q.getBuy()) + ")"
				+ marginTail(keep - offerPrice) + ".";
		}
		return "✓ Hold " + item + progress + ": your " + PriceFormat.gp(offerPrice)
			+ " is in line with the market (" + PriceFormat.gp(q.getSell()) + "). You keep "
			+ PriceFormat.gp(BankView.afterTax(offerPrice)) + " each.";
	}

	private static String marginTail(long each)
	{
		if (each == 0)
		{
			return "";
		}
		return ". Selling at today's price makes " + signed(each) + " each after tax";
	}

	private static String pct(long high, long low)
	{
		if (low <= 0)
		{
			return "";
		}
		double p = (high - low) * 100.0 / low;
		return (p < 10 ? String.format("%.1f", p) : String.valueOf(Math.round(p))) + "%";
	}

	private static String signed(long v)
	{
		return (v > 0 ? "+" : "") + PriceFormat.gp(v);
	}

	private static String label(String name)
	{
		return name == null || name.isEmpty() ? "That item" : name;
	}
}
