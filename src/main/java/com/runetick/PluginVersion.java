package com.runetick;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** The build number from runelite-plugin.properties, which the jar also ships. */
final class PluginVersion
{
	private PluginVersion()
	{
	}

	static String text()
	{
		try (InputStream in = PluginVersion.class.getResourceAsStream("/runelite-plugin.properties"))
		{
			if (in == null)
			{
				return "";
			}
			Properties props = new Properties();
			props.load(in);
			String version = props.getProperty("version");
			return version == null ? "" : version.trim();
		}
		catch (IOException e)
		{
			return "";
		}
	}
}
