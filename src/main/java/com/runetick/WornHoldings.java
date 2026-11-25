package com.runetick;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;

/**
 * Worn equipment quantities (canonical ids). Worn gear is part of what a player owns.
 */
@Singleton
public class WornHoldings extends InventoryHoldings
{
	@Inject
	public WornHoldings(ItemManager itemManager)
	{
		super(itemManager);
	}
}
