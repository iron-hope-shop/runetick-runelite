package com.runetick;

import com.google.gson.Gson;
import java.util.List;
import net.runelite.client.config.ConfigManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Test;

public class GeHistoryTest
{
	@Test
	public void parsesRuneLiteShortFieldHistory()
	{
		ConfigManager configs = mock(ConfigManager.class);
		when(configs.getRSProfileConfiguration("grandexchange", "tradeHistory"))
			.thenReturn("[{\"b\":true,\"i\":8790,\"q\":4196,\"p\":350,\"t\":1795066299437},"
				+ "{\"b\":false,\"i\":12782,\"q\":43,\"p\":958,\"t\":1795066447753}]");

		GeHistory history = new GeHistory(configs, new Gson());
		List<GeTrade> recent = history.recent(8);
		assertEquals(2, recent.size());
		assertTrue(recent.get(0).isBuy());
		assertEquals(8790, recent.get(0).getItemId());
		assertEquals(4196, recent.get(0).getQuantity());
		assertEquals(350L, recent.get(0).getPrice());
		assertEquals(false, recent.get(1).isBuy());
	}

	@Test
	public void emptyWhenMissing()
	{
		ConfigManager configs = mock(ConfigManager.class);
		when(configs.getRSProfileConfiguration("grandexchange", "tradeHistory")).thenReturn(null);
		assertTrue(new GeHistory(configs, new Gson()).recent(5).isEmpty());
	}
}
