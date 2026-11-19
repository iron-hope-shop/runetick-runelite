package com.runetick;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Stores the last logged offer per GE slot against the current RuneScape account,
 * so an uncollected finished offer is not logged again on the next login.
 */
@Singleton
public class ConfigLoggedOffers implements OfferSync.LoggedOffers
{
	private static final String KEY_PREFIX = "loggedOffer.";

	private final ConfigManager configManager;

	@Inject
	public ConfigLoggedOffers(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	@Override
	public String get(int slot)
	{
		return configManager.getRSProfileConfiguration(RuneTickConfig.GROUP, KEY_PREFIX + slot);
	}

	@Override
	public void put(int slot, String fingerprint)
	{
		configManager.setRSProfileConfiguration(RuneTickConfig.GROUP, KEY_PREFIX + slot, fingerprint);
	}

	@Override
	public void clear(int slot)
	{
		configManager.unsetRSProfileConfiguration(RuneTickConfig.GROUP, KEY_PREFIX + slot);
	}
}
