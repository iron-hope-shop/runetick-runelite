package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.util.Filepath;

/**
 * Runetick sign-in stays in the plugin directory, not in a synced RuneLite profile.
 */
@Singleton
public class SessionStore
{
	@Value
	public static class Session
	{
		String apiKey;
		String refreshToken;
	}

	private final PluginFiles files;
	private final Gson gson;

	@Inject
	public SessionStore(PluginFiles files, Gson gson)
	{
		this.files = files;
		this.gson = gson;
	}

	public synchronized Session load() throws IOException
	{
		Filepath file = files.file("session.json");
		if (!file.exists())
		{
			return null;
		}
		try (Reader reader = file.openBufferedReader())
		{
			Session s = gson.fromJson(reader, Session.class);
			if (s == null || isBlank(s.getApiKey()) || isBlank(s.getRefreshToken()))
			{
				return null;
			}
			return s;
		}
		catch (JsonSyntaxException e)
		{
			return null;
		}
	}

	public synchronized void save(Session session) throws IOException
	{
		files.file("session.json").write(gson.toJson(session));
	}

	public synchronized void clear() throws IOException
	{
		files.file("session.json").deleteIfExists();
	}

	private static boolean isBlank(String s)
	{
		return s == null || s.trim().isEmpty();
	}
}
