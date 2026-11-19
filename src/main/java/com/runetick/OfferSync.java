package com.runetick;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Decides what a Grand Exchange offer update means for the player's Runetick account:
 * progress on a flip they placed there, or a finished trade for their trade log.
 * Calls block; use a background thread.
 */
@Singleton
public class OfferSync
{
	/** Remembers which finished offer was last logged per GE slot, across logins. */
	public interface LoggedOffers
	{
		String get(int slot);

		void put(int slot, String fingerprint);

		void clear(int slot);
	}

	public enum Result
	{
		IGNORED, PLACED_UPDATED, TRADE_LOGGED
	}

	private final RuneTickApi api;
	private final RuneTickConfig config;
	private final FirebaseSession session;
	private final LoggedOffers logged;

	@Inject
	public OfferSync(RuneTickApi api, RuneTickConfig config, FirebaseSession session, LoggedOffers logged)
	{
		this.api = api;
		this.config = config;
		this.session = session;
		this.logged = logged;
	}

	public Result handle(OfferEvent event, String itemName) throws IOException
	{
		int filled = event.getQuantityFilled();
		if (filled <= 0 || !session.isLinked())
		{
			return Result.IGNORED;
		}

		if (config.syncPlaced())
		{
			RuneTickApi.PlacedFlip flip = findFlip(api.getPlaced(), event);
			if (flip != null)
			{
				// Runetick logs the trade itself when the flip completes, so never log it here too.
				return updateFlip(flip, event) ? Result.PLACED_UPDATED : Result.IGNORED;
			}
		}

		if (config.logTrades() && event.isFinished())
		{
			String fingerprint = event.fingerprint();
			if (fingerprint.equals(logged.get(event.getSlot())))
			{
				return Result.IGNORED;
			}
			api.logTrade(event.getItemId(), itemName, event.isBuy(), filled, event.unitPrice());
			logged.put(event.getSlot(), fingerprint);
			return Result.TRADE_LOGGED;
		}

		return Result.IGNORED;
	}

	/** The slot was emptied in game, so the next offer in it is a new one even if it looks identical. */
	public void slotEmptied(int slot)
	{
		logged.clear(slot);
	}

	/**
	 * The oldest placed flip for this item that is still waiting on this side of the trade.
	 * The API lists flips newest first.
	 */
	private static RuneTickApi.PlacedFlip findFlip(List<RuneTickApi.PlacedFlip> placed, OfferEvent event)
	{
		RuneTickApi.PlacedFlip match = null;
		for (RuneTickApi.PlacedFlip f : placed)
		{
			if (f.getItemId() != event.getItemId())
			{
				continue;
			}
			boolean waiting = event.isBuy() ? f.getBought() < f.getUnits() : f.getSold() < f.getUnits();
			if (waiting)
			{
				match = f;
			}
		}
		return match;
	}

	/**
	 * Counts only ever go up: offers are re-sent on every login and must not undo progress.
	 */
	private boolean updateFlip(RuneTickApi.PlacedFlip flip, OfferEvent event) throws IOException
	{
		int filled = Math.min(flip.getUnits(), event.getQuantityFilled());
		if (event.isBuy())
		{
			if (filled <= flip.getBought())
			{
				return false;
			}
			api.updatePlaced(flip.getId(), filled, null);
			return true;
		}

		if (filled <= flip.getSold())
		{
			return false;
		}
		// Runetick caps sold at bought; if the buy happened before the plugin was watching, catch bought up.
		Integer bought = filled > flip.getBought() ? filled : null;
		api.updatePlaced(flip.getId(), bought, filled);
		return true;
	}
}
