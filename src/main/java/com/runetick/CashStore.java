package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import net.runelite.client.util.Filepath;

/**
 * Last-known inventory and bank cash, so the panel survives a restart.
 */
@Singleton
public class CashStore
{
	@Data
	public static class Snapshot
	{
		private long inventoryGp;
		private long bankGp;
		private boolean bankSeen;
		private long savedAt;
	}

	private final PluginFiles files;
	private final Gson gson;

	@Inject
	public CashStore(PluginFiles files, Gson gson)
	{
		this.files = files;
		this.gson = gson;
	}

	public synchronized Snapshot load()
	{
		try
		{
			Filepath file = files.file("cash.json");
			if (!file.exists())
			{
				return null;
			}
			try (Reader reader = file.openBufferedReader())
			{
				Snapshot s = gson.fromJson(reader, Snapshot.class);
				if (s == null)
				{
					return null;
				}
				s.setInventoryGp(Math.max(0, s.getInventoryGp()));
				s.setBankGp(Math.max(0, s.getBankGp()));
				return s;
			}
		}
		catch (IOException | JsonSyntaxException e)
		{
			return null;
		}
	}

	public synchronized void save(long inventoryGp, long bankGp, boolean bankSeen) throws IOException
	{
		Snapshot s = new Snapshot();
		s.setInventoryGp(Math.max(0, inventoryGp));
		s.setBankGp(Math.max(0, bankGp));
		s.setBankSeen(bankSeen);
		s.setSavedAt(System.currentTimeMillis());
		files.file("cash.json").write(gson.toJson(s));
	}
}
