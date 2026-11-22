package com.runetick;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Shared, thread-safe snapshot of what the plugin knows about the player's Runetick account.
 * Written from the background executor, read by the overlays on the client thread.
 */
@Singleton
public class RuneTickState
{
	private volatile boolean linked;
	private volatile List<RuneTickApi.PlacedFlip> placed = Collections.emptyList();
	private volatile RuneTickApi.PlacedSummary summary;
	private volatile List<RuneTickApi.PlanPosition> recommendations = Collections.emptyList();
	private volatile List<RuneTickApi.FatSpread> fat = Collections.emptyList();
	private volatile List<RuneTickApi.Dip> dips = Collections.emptyList();
	private volatile String planPace = "";
	private volatile long planGpPerHour;
	private volatile List<RuneTickApi.Signal> cheap = Collections.emptyList();
	private volatile List<RuneTickApi.Signal> dear = Collections.emptyList();
	private volatile RuneTickApi.ItemQuote geItem;
	private volatile int geItemId;
	private volatile String note = "";
	private volatile long inventoryGp;
	private volatile long bankGp;
	private volatile boolean bankSeen;
	private volatile List<GeTrade> recentTrades = Collections.emptyList();
	private volatile boolean loadingBoard;
	private volatile boolean loadingFlips;
	private volatile boolean syncing;
	private volatile boolean loggedIn;
	private volatile String syncStatus = "";
	private volatile Map<Integer, BankView.Mark> valueMarks = Collections.emptyMap();
	private volatile Map<Integer, BankView.Mark> profitMarks = Collections.emptyMap();

	public Map<Integer, BankView.Mark> getValueMarks()
	{
		return valueMarks;
	}

	public Map<Integer, BankView.Mark> getProfitMarks()
	{
		return profitMarks;
	}

	public void setItemMarks(Map<Integer, BankView.Mark> value, Map<Integer, BankView.Mark> profit)
	{
		valueMarks = value == null ? Collections.<Integer, BankView.Mark>emptyMap() : value;
		profitMarks = profit == null ? Collections.<Integer, BankView.Mark>emptyMap() : profit;
	}

	public boolean isLinked()
	{
		return linked;
	}

	public void setLinked(boolean linked)
	{
		this.linked = linked;
		if (!linked)
		{
			placed = Collections.emptyList();
			summary = null;
			recommendations = Collections.emptyList();
			fat = Collections.emptyList();
			dips = Collections.emptyList();
			planPace = "";
			planGpPerHour = 0;
			cheap = Collections.emptyList();
			dear = Collections.emptyList();
			geItem = null;
			geItemId = 0;
			recentTrades = Collections.emptyList();
			loadingBoard = false;
			loadingFlips = false;
			syncing = false;
			syncStatus = "";
		}
	}

	public boolean isLoadingBoard()
	{
		return loadingBoard;
	}

	public void setLoadingBoard(boolean loadingBoard)
	{
		this.loadingBoard = loadingBoard;
	}

	public boolean isLoadingFlips()
	{
		return loadingFlips;
	}

	public void setLoadingFlips(boolean loadingFlips)
	{
		this.loadingFlips = loadingFlips;
	}

	public boolean isSyncing()
	{
		return syncing;
	}

	public void setSyncing(boolean syncing)
	{
		this.syncing = syncing;
	}

	public boolean isLoggedIn()
	{
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
	}

	public String getSyncStatus()
	{
		return syncStatus;
	}

	public void setSyncStatus(String syncStatus)
	{
		this.syncStatus = syncStatus == null ? "" : syncStatus;
	}

	public List<RuneTickApi.FatSpread> getFat()
	{
		return fat;
	}

	public List<RuneTickApi.Dip> getDips()
	{
		return dips;
	}

	public void setDips(List<RuneTickApi.Dip> dips)
	{
		this.dips = dips == null ? Collections.emptyList() : dips;
	}

	public long getInventoryGp()
	{
		return inventoryGp;
	}

	public long getBankGp()
	{
		return bankGp;
	}

	public boolean isBankSeen()
	{
		return bankSeen;
	}

	public void setCash(long inventoryGp, long bankGp, boolean bankSeen)
	{
		this.inventoryGp = Math.max(0, inventoryGp);
		this.bankGp = Math.max(0, bankGp);
		this.bankSeen = bankSeen;
	}

	public List<GeTrade> getRecentTrades()
	{
		return recentTrades;
	}

	public void setRecentTrades(List<GeTrade> trades)
	{
		this.recentTrades = trades == null ? Collections.emptyList() : trades;
	}

	public List<RuneTickApi.PlacedFlip> getPlaced()
	{
		return placed;
	}

	public RuneTickApi.PlacedSummary getSummary()
	{
		return summary;
	}

	public void setPlaced(List<RuneTickApi.PlacedFlip> placed, RuneTickApi.PlacedSummary summary)
	{
		this.placed = placed == null ? Collections.emptyList() : placed;
		this.summary = summary;
	}

	public List<RuneTickApi.PlanPosition> getRecommendations()
	{
		return recommendations;
	}

	public String getPlanPace()
	{
		return planPace;
	}

	public long getPlanGpPerHour()
	{
		return planGpPerHour;
	}

	public void setPlan(RuneTickApi.Plan plan)
	{
		if (plan == null)
		{
			recommendations = Collections.emptyList();
			fat = Collections.emptyList();
			planPace = "";
			planGpPerHour = 0;
			return;
		}
		recommendations = plan.getPositions() == null ? Collections.emptyList() : plan.getPositions();
		fat = plan.getFat() == null ? Collections.emptyList() : plan.getFat();
		planPace = plan.getPace() == null ? "" : plan.getPace();
		long suggested = plan.getSuggestedGpPerHour();
		planGpPerHour = suggested > 0 ? suggested : plan.getExpectedGpPerHour();
	}

	public List<RuneTickApi.Signal> getCheap()
	{
		return cheap;
	}

	public List<RuneTickApi.Signal> getDear()
	{
		return dear;
	}

	public void setSignals(RuneTickApi.Signals signals)
	{
		if (signals == null)
		{
			cheap = Collections.emptyList();
			dear = Collections.emptyList();
			return;
		}
		cheap = signals.getCheap() == null ? Collections.emptyList() : signals.getCheap();
		dear = signals.getDear() == null ? Collections.emptyList() : signals.getDear();
	}

	public RuneTickApi.ItemQuote getGeItem()
	{
		return geItem;
	}

	public int getGeItemId()
	{
		return geItemId;
	}

	public void setGeItem(int itemId, RuneTickApi.ItemQuote quote)
	{
		geItemId = itemId;
		geItem = quote;
	}

	public void clearGeItem()
	{
		geItemId = 0;
		geItem = null;
	}

	public String getNote()
	{
		return note;
	}

	public void setNote(String note)
	{
		this.note = note == null ? "" : note;
	}

	/** Placed flip for an item, if any (prefers open flips). */
	public RuneTickApi.PlacedFlip placedFor(int itemId)
	{
		RuneTickApi.PlacedFlip match = null;
		for (RuneTickApi.PlacedFlip f : placed)
		{
			if (f.getItemId() == itemId)
			{
				if (!"done".equals(f.getStatus()))
				{
					return f;
				}
				match = f;
			}
		}
		return match;
	}

	/** Plan recommendation for an item, if any. */
	public RuneTickApi.PlanPosition recommendationFor(int itemId)
	{
		for (RuneTickApi.PlanPosition p : recommendations)
		{
			if (p.getId() == itemId)
			{
				return p;
			}
		}
		return null;
	}

	public RuneTickApi.Signal signalFor(int itemId)
	{
		for (RuneTickApi.Signal s : cheap)
		{
			if (s.getId() == itemId)
			{
				return s;
			}
		}
		for (RuneTickApi.Signal s : dear)
		{
			if (s.getId() == itemId)
			{
				return s;
			}
		}
		return null;
	}

	public RuneTickApi.FatSpread fatFor(int itemId)
	{
		for (RuneTickApi.FatSpread f : fat)
		{
			if (f.getId() == itemId)
			{
				return f;
			}
		}
		return null;
	}

	public RuneTickApi.Dip dipFor(int itemId)
	{
		for (RuneTickApi.Dip d : dips)
		{
			if (d.getId() == itemId)
			{
				return d;
			}
		}
		return null;
	}

	/** Item ids tagged because they are one of the player's own open flips. */
	public Map<Integer, String> highlightLabels()
	{
		Map<Integer, String> out = new HashMap<>();
		for (RuneTickApi.PlacedFlip f : placed)
		{
			if ("done".equals(f.getStatus()))
			{
				continue;
			}
			// Still buying the stack → tag the buy price; otherwise tag the hold-sell price.
			if ("selling".equals(f.getStatus()) || f.getBought() >= f.getUnits())
			{
				out.put(f.getItemId(), "sell " + PriceFormat.gp(f.getAsk()));
			}
			else
			{
				out.put(f.getItemId(), "buy " + PriceFormat.gp(f.getBid()));
			}
		}
		return out;
	}
}
