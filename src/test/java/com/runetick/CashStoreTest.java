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

	@Test
	public void eachCharacterHasItsOwnCashAndBankAndTheOldFileGoesToTheFirstOne() throws Exception
	{
		Path dir = tmp.newFolder("chars").toPath();
		PluginFiles files = new PluginFiles();
		files.use(Filepath.Unchecked.getRooted(dir));
		CashStore store = new CashStore(files, new Gson());
		store.save(5, 7_000_000, true);
		store.useAccount(111L);
		CashStore.Snapshot main = store.load();
		assertEquals(7_000_000, main.getBankGp());
		assertTrue(files.file("cash-111.json").exists());
		assertTrue(!files.file("cash.json").exists());
		store.saveBankItems(java.util.Collections.singletonMap(4151, 2));
		assertEquals(Integer.valueOf(2), store.loadBankItems().get(4151));
		store.useAccount(222L);
		assertEquals(null, store.load());
		assertEquals(null, store.loadBankItems());
		store.save(1, 2, true);
		store.useAccount(111L);
		assertEquals(7_000_000, store.load().getBankGp());
	}
}
