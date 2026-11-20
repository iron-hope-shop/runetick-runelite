package com.runetick;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/** Appends exceptions to error.log in the plugin directory. */
@Slf4j
@Singleton
public class ErrorLog
{
	private final PluginFiles files;

	@Inject
	public ErrorLog(PluginFiles files)
	{
		this.files = files;
	}

	public synchronized void log(String message, Throwable t)
	{
		log.warn("{}", message, t);

		StringWriter sw = new StringWriter();
		sw.append(Instant.now().toString()).append(' ').append(message).append(System.lineSeparator());
		if (t != null)
		{
			t.printStackTrace(new PrintWriter(sw));
		}

		try
		{
			Filepath file = files.file("error.log");
			file.write(readText(file) + sw);
		}
		catch (IOException e)
		{
			log.warn("Unable to write error.log", e);
		}
	}

	private static String readText(Filepath file) throws IOException
	{
		if (!file.exists())
		{
			return "";
		}
		StringBuilder prior = new StringBuilder();
		char[] buf = new char[4096];
		try (Reader reader = file.openReader())
		{
			int n;
			while ((n = reader.read(buf)) >= 0)
			{
				prior.append(buf, 0, n);
			}
		}
		return prior.toString();
	}
}
