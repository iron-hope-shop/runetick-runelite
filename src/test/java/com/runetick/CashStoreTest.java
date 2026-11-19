package com.runetick;

import com.google.gson.Gson;
import java.nio.file.Path;
import net.runelite.client.util.Filepath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CashStoreTest
{
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void roundTripsCashSnapshot() throws Exception
	{
		Path dir = tmp.newFolder("data").toPath();
		PluginFiles files = new PluginFiles();
		files.use(Filepath.Unchecked.getRooted(dir));
		CashStore store = new CashStore(files, new Gson());
		store.save(12_345, 99_000_000, true);
		assertTrue(files.file("cash.json").exists());
		CashStore.Snapshot snap = store.load();
		assertNotNull(snap);
		assertEquals(12_345, snap.getInventoryGp());
		assertEquals(99_000_000, snap.getBankGp());
		assertTrue(snap.isBankSeen());
		assertTrue(snap.getSavedAt() > 0);
	}
}
