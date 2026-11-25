package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import net.runelite.client.util.Filepath;

/**
 * Last-known inventory and bank cash, and the last-seen bank items, so the panel survives a restart.
 * Each character has its own files, so an alt never shows another character's bank.
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

	@Data
	public static class BankItems
	{
		private Map<Integer, Integer> items;
		private long savedAt;
	}

	/** Use this character's files from now on. A hash of -1 or 0 means no character. */
	public synchronized void useAccount(long accountHash)
	{
		account = accountHash == -1 || accountHash == 0 ? "" : Long.toUnsignedString(accountHash);
	}

	public String getAccount()
	{
		return account;
	}

	static String cashFile(String account)
	{
		return account.isEmpty() ? "cash.json" : "cash-" + account + ".json";
	}

	static String bankFile(String account)
	{
		return account.isEmpty() ? "bank.json" : "bank-" + account + ".json";
	}

	private final PluginFiles files;
	private final Gson gson;
	private volatile String account = "";

	@Inject
	public CashStore(PluginFiles files, Gson gson)
	{
		this.files = files;
		this.gson = gson;
	}

	public synchronized Snapshot load()
	{
		Snapshot own = read(cashFile(account), Snapshot.class);
		if (own != null || account.isEmpty())
		{
			return clean(own);
		}
		Snapshot legacy = read(cashFile(""), Snapshot.class);
		if (legacy == null)
		{
			return null;
		}
		try
		{
			files.file(cashFile(account)).write(gson.toJson(legacy));
			files.file(cashFile("")).delete();
		}
		catch (IOException e)
		{
			return clean(legacy);
		}
		return clean(legacy);
	}

	private static Snapshot clean(Snapshot s)
	{
		if (s == null)
		{
			return null;
		}
		s.setInventoryGp(Math.max(0, s.getInventoryGp()));
		s.setBankGp(Math.max(0, s.getBankGp()));
		return s;
	}

	public synchronized Map<Integer, Integer> loadBankItems()
	{
		if (account.isEmpty())
		{
			return null;
		}
		BankItems b = read(bankFile(account), BankItems.class);
		if (b == null || b.getItems() == null)
		{
			return null;
		}
		Map<Integer, Integer> out = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : b.getItems().entrySet())
		{
			if (e.getKey() != null && e.getKey() > 0 && e.getValue() != null && e.getValue() > 0)
			{
				out.put(e.getKey(), e.getValue());
			}
		}
		return out;
	}

	public synchronized void saveBankItems(Map<Integer, Integer> items) throws IOException
	{
		if (account.isEmpty() || items == null)
		{
			return;
		}
		BankItems b = new BankItems();
		b.setItems(new HashMap<>(items));
		b.setSavedAt(System.currentTimeMillis());
		files.file(bankFile(account)).write(gson.toJson(b));
	}

	private <T> T read(String name, Class<T> type)
	{
		try
		{
			Filepath file = files.file(name);
			if (!file.exists())
			{
				return null;
			}
			try (Reader reader = file.openBufferedReader())
			{
				return gson.fromJson(reader, type);
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
		files.file(cashFile(account)).write(gson.toJson(s));
	}
}
