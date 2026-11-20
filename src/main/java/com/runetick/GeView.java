package com.runetick;

import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

/** Which Grand Exchange item the player is actually looking at. */
final class GeView
{
	private GeView()
	{
	}

	static boolean offerScreenOpen(Client client)
	{
		return shown(client.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER))
			|| shown(client.getWidget(InterfaceID.GeOffers.DETAILS))
			|| shown(client.getWidget(InterfaceID.GeOffers.SETUP));
	}

	private static boolean shown(Widget widget)
	{
		return widget != null && !widget.isHidden();
	}

	/** Selected slot, or -1 on the GE home screen. */
	static int selectedSlot(Client client)
	{
		int slot = client.getVarbitValue(VarbitID.GE_SELECTEDSLOT) - 1;
		return slot >= 0 && slot <= 7 ? slot : -1;
	}

	/**
	 * Item on the buy/sell page. An open offer uses that slot's item.
	 * A new offer uses the item just chosen. Home screen returns 0.
	 */
	static int tipItemId(Client client)
	{
		if (!offerScreenOpen(client))
		{
			return 0;
		}
		// The offer you just placed lives on the details page, not in the search varp.
		int fromScreen = itemOn(client.getWidget(InterfaceID.GeOffers.DETAILS));
		if (fromScreen <= 0)
		{
			fromScreen = itemOn(client.getWidget(InterfaceID.GeOffers.SETUP));
		}
		if (fromScreen > 0)
		{
			return fromScreen;
		}
		int fromSlot = itemInSlot(client, selectedSlot(client));
		if (fromSlot > 0)
		{
			return fromSlot;
		}
		return Math.max(0, client.getVarpValue(VarPlayer.CURRENT_GE_ITEM));
	}

	/** Item icon drawn on a setup or details panel. */
	private static int itemOn(Widget widget)
	{
		return itemOn(widget, 0);
	}

	private static int itemOn(Widget widget, int depth)
	{
		if (!shown(widget) || depth > 8)
		{
			return 0;
		}
		if (widget.getItemId() > 0)
		{
			return widget.getItemId();
		}
		int found = firstItem(widget.getStaticChildren(), depth);
		if (found > 0)
		{
			return found;
		}
		found = firstItem(widget.getDynamicChildren(), depth);
		if (found > 0)
		{
			return found;
		}
		found = firstItem(widget.getNestedChildren(), depth);
		if (found > 0)
		{
			return found;
		}
		return firstItem(widget.getChildren(), depth);
	}

	private static int firstItem(Widget[] children, int depth)
	{
		if (children == null)
		{
			return 0;
		}
		for (Widget child : children)
		{
			int id = itemOn(child, depth + 1);
			if (id > 0)
			{
				return id;
			}
		}
		return 0;
	}

	static int itemInSlot(Client client, int slot)
	{
		if (slot < 0)
		{
			return 0;
		}
		GrandExchangeOffer[] offers;
		try
		{
			offers = client.getGrandExchangeOffers();
		}
		catch (Throwable t)
		{
			return 0;
		}
		if (offers == null || slot >= offers.length)
		{
			return 0;
		}
		GrandExchangeOffer offer = offers[slot];
		if (offer == null || offer.getState() == GrandExchangeOfferState.EMPTY || offer.getItemId() <= 0)
		{
			return 0;
		}
		return offer.getItemId();
	}

	/** Slot whose widget owns this menu click, or -1. */
	static int slotFromMenu(Client client, int widgetId)
	{
		for (int slot = 0; slot < 8; slot++)
		{
			Widget slotWidget = client.getWidget(InterfaceID.GE_OFFERS, 7 + slot);
			if (slotWidget == null)
			{
				continue;
			}
			if (slotWidget.getId() == widgetId)
			{
				return slot;
			}
			Widget[] children = slotWidget.getDynamicChildren();
			if (children == null)
			{
				children = slotWidget.getChildren();
			}
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				if (child != null && child.getId() == widgetId)
				{
					return slot;
				}
			}
		}
		return -1;
	}
}
