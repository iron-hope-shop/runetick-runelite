package com.runetick;

import lombok.Value;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/**
 * Immutable snapshot of a Grand Exchange offer.
 */
@Value
public class OfferEvent
{
	int slot;
	int itemId;
	GrandExchangeOfferState state;
	long price;
	int totalQuantity;
	int quantityFilled;
	long spent;
	long timestamp;

	public static OfferEvent of(int slot, GrandExchangeOffer offer, long timestampMillis)
	{
		return new OfferEvent(
			slot,
			offer.getItemId(),
			offer.getState(),
			offer.getPrice(),
			offer.getTotalQuantity(),
			offer.getQuantitySold(),
			offer.getSpent(),
			timestampMillis
		);
	}

	public boolean isBuy()
	{
		return state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;
	}

	/**
	 * True once the offer can no longer fill: completed or cancelled.
	 */
	public boolean isFinished()
	{
		return state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.SOLD
			|| state == GrandExchangeOfferState.CANCELLED_BUY
			|| state == GrandExchangeOfferState.CANCELLED_SELL;
	}

	/**
	 * Average gp actually paid or received per item (before tax), or 0 if nothing filled.
	 */
	public long unitPrice()
	{
		return quantityFilled > 0 ? Math.round((double) spent / quantityFilled) : 0;
	}

	/**
	 * Identifies this exact finished offer, so it is only logged once across logins.
	 */
	public String fingerprint()
	{
		return itemId + ":" + state + ":" + quantityFilled + ":" + spent;
	}

	/**
	 * True when both events describe the same offer state, ignoring the timestamp.
	 */
	public boolean sameOfferAs(OfferEvent other)
	{
		return other != null
			&& slot == other.slot
			&& itemId == other.itemId
			&& state == other.state
			&& price == other.price
			&& totalQuantity == other.totalQuantity
			&& quantityFilled == other.quantityFilled
			&& spent == other.spent;
	}
}
