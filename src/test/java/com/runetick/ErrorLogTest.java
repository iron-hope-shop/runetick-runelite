package com.runetick;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.runelite.client.util.Filepath;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ErrorLogTest
{
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void appendsEachException() throws Exception
	{
		PluginFiles files = new PluginFiles();
		files.use(Filepath.Unchecked.getRooted(tmp.getRoot().toPath()));
		ErrorLog log = new ErrorLog(files);

		log.log("first", new IllegalStateException("one"));
		log.log("second", new IllegalStateException("two"));

		String text = new String(Files.readAllBytes(Filepath.Unchecked.getPath(files.file("error.log"))), StandardCharsets.UTF_8);
		assertTrue(text.contains("first"));
		assertTrue(text.contains("one"));
		assertTrue(text.contains("second"));
		assertTrue(text.contains("two"));
		assertTrue(text.indexOf("first") < text.indexOf("second"));
	}
}
