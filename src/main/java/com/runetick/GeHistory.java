package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Reads completed GE fills persisted by RuneLite's built-in Grand Exchange plugin.
 */
@Singleton
public class GeHistory
{
	private static final String GROUP = "grandexchange";
	private static final String KEY = "tradeHistory";

	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	public GeHistory(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	public List<GeTrade> all()
	{
		return recent(Integer.MAX_VALUE);
	}

	public List<GeTrade> recent(int limit)
	{
		String json = configManager.getRSProfileConfiguration(GROUP, KEY);
		if (json == null || json.isEmpty())
		{
			return Collections.emptyList();
		}
		try
		{
			List<GeTrade> trades = readTrades(json);
			if (trades == null || trades.isEmpty())
			{
				return Collections.emptyList();
			}
			int from = Math.max(0, trades.size() - Math.max(1, limit));
			return trades.subList(from, trades.size());
		}
		catch (JsonSyntaxException e)
		{
			return Collections.emptyList();
		}
	}

	private List<GeTrade> readTrades(String json)
	{
		JsonArray array = new JsonParser().parse(json).getAsJsonArray();
		List<GeTrade> trades = new ArrayList<>();
		for (JsonElement element : array)
		{
			GeTrade trade = gson.fromJson(element, GeTrade.class);
			if (trade != null)
			{
				trades.add(trade);
			}
		}
		return trades;
	}
}
