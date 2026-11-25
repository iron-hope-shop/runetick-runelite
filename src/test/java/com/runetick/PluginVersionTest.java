package com.runetick;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PluginVersionTest
{
	@Test
	public void readsThePluginProperties()
	{
		assertEquals("0.4.26", PluginVersion.text());
	}
}
