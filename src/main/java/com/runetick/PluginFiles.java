package com.runetick;

import java.io.IOException;
import javax.inject.Singleton;
import net.runelite.client.util.Filepath;

/**
 * Plugin data directory from {@code Plugin#getPluginDirectory()}.
 * Call {@link #use} during startup before any file is read.
 */
@Singleton
public class PluginFiles
{
	private Filepath root;

	public void use(Filepath root)
	{
		this.root = root;
	}

	public Filepath file(String name) throws IOException
	{
		if (root == null)
		{
			throw new IOException("plugin directory is not ready");
		}
		if (!root.exists())
		{
			root.createDirectories();
		}
		return root.joinSegment(name);
	}
}
