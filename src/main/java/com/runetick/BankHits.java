package com.runetick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Board opportunities that overlap the player's last-seen bank stack.
 */
final class BankHits
{
	private BankHits()
	{
	}

	static List<BoardLine> build(
		Map<Integer, Integer> holdings,
		List<RuneTickApi.FatSpread> fat,
		List<RuneTickApi.Dip> dips,
		List<RuneTickApi.PlacedFlip> placed,
		long minDreamGp,
		long minMarginPerUnit,
		boolean hideAlch,
		int limit)
	{
		List<Hit> hits = new ArrayList<>();
		if (holdings == null || holdings.isEmpty())
		{
			return java.util.Collections.emptyList();
		}

		if (fat != null)
		{
			for (RuneTickApi.FatSpread f : fat)
			{
				if (f == null)
				{
					continue;
				}
				int q = holdings.getOrDefault(f.getId(), 0);
				if (q <= 0)
				{
					continue;
				}
				if (f.getDreamGp() < minDreamGp || f.getMarginPerUnit() < minMarginPerUnit)
				{
					continue;
				}
				hits.add(new Hit(
					f.getId(),
					f.getName(),
					"fat buy " + PriceFormat.gp(f.getBid()) + " -> sell " + PriceFormat.gp(f.getAsk()),
					"have " + q + " | profit " + PriceFormat.gp(f.getDreamGp()),
					f.getDreamGp()));
			}
		}

		if (dips != null)
		{
			for (RuneTickApi.Dip d : dips)
			{
				if (d == null)
				{
					continue;
				}
				if (hideAlch && d.isAlchFloor())
				{
					continue;
				}
				int q = holdings.getOrDefault(d.getId(), 0);
				if (q <= 0)
				{
					continue;
				}
				long ask = FocusCopy.dipAsk(d.getPrice());
				long margin = ask - d.getPrice();
				if (margin < minMarginPerUnit)
				{
					continue;
				}
				hits.add(new Hit(
					d.getId(),
					d.getName(),
					"dip buy " + PriceFormat.gp(d.getPrice()) + " -> sell " + PriceFormat.gp(ask),
					"have " + q + " | margin " + PriceFormat.gp(margin) + "/ea",
					margin * Math.max(1, q)));
			}
		}

		if (placed != null)
		{
			for (RuneTickApi.PlacedFlip f : placed)
			{
				if (f == null || "done".equals(f.getStatus()))
				{
					continue;
				}
				int q = holdings.getOrDefault(f.getItemId(), 0);
				if (q <= 0)
				{
					continue;
				}
				hits.add(new Hit(
					f.getItemId(),
					f.getItemName(),
					"flip buy " + PriceFormat.gp(f.getBid()) + " -> sell " + PriceFormat.gp(f.getAsk()),
					"have " + q + " | fill " + f.getBought() + "/" + f.getUnits(),
					f.getProfitIfDone()));
			}
		}

		hits.sort(Comparator.comparingLong((Hit h) -> h.score).reversed());
		List<BoardLine> out = new ArrayList<>();
		for (Hit h : hits)
		{
			if (out.size() >= Math.max(1, limit))
			{
				break;
			}
			// Dedupe by item — keep highest-scoring label.
			boolean seen = false;
			for (BoardLine existing : out)
			{
				if (existing.itemId == h.itemId)
				{
					seen = true;
					break;
				}
			}
			if (seen)
			{
				continue;
			}
			out.add(BoardLine.of(h.itemId, h.name, h.mid, h.total));
		}
		return out;
	}

	private static final class Hit
	{
		final int itemId;
		final String name;
		final String mid;
		final String total;
		final long score;

		Hit(int itemId, String name, String mid, String total, long score)
		{
			this.itemId = itemId;
			this.name = name;
			this.mid = mid;
			this.total = total;
			this.score = score;
		}
	}
}
