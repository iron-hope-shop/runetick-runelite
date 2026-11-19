package com.runetick;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Starts a full RuneLite client with the plugin loaded: ./gradlew run
 */
public class RuneTickPluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RuneTickPlugin.class);
		RuneLite.main(args);
	}
}
