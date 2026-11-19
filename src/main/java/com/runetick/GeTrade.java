package com.runetick;

import com.google.gson.annotations.SerializedName;

/**
 * One completed GE fill as stored by RuneLite's Grand Exchange plugin
 * ({@code grandexchange.tradeHistory}).
 */
public class GeTrade
{
	@SerializedName("b")
	private boolean buy;

	@SerializedName("i")
	private int itemId;

	@SerializedName("q")
	private int quantity;

	@SerializedName("p")
	private long price;

	/** Epoch millis. */
	@SerializedName("t")
	private long time;

	public boolean isBuy()
	{
		return buy;
	}

	public int getItemId()
	{
		return itemId;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public long getPrice()
	{
		return price;
	}

	public long getTime()
	{
		return time;
	}

	public long getValue()
	{
		return price * (long) quantity;
	}
}
