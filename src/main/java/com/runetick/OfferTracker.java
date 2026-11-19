package com.runetick;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/**
 * Turns raw Grand Exchange offer updates into {@link OfferEvent}s, dropping
 * empty slots and repeats (the client re-sends every slot on each login).
 */
public class OfferTracker
{
	private final Map<Integer, OfferEvent> lastBySlot = new HashMap<>();

	/**
	 * @return the event to export, or null if this update should be ignored
	 */
	public synchronized OfferEvent track(int slot, GrandExchangeOffer offer, long timestampMillis)
	{
		if (offer == null || offer.getState() == null)
		{
			return null;
		}

		if (offer.getState() == GrandExchangeOfferState.EMPTY)
		{
			lastBySlot.remove(slot);
			return null;
		}

		OfferEvent event = OfferEvent.of(slot, offer, timestampMillis);
		if (event.sameOfferAs(lastBySlot.get(slot)))
		{
			return null;
		}

		lastBySlot.put(slot, event);
		return event;
	}

	public synchronized void reset()
	{
		lastBySlot.clear();
	}
}
