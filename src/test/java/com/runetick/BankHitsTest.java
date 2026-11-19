package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class BankHitsTest
{
	@Test
	public void matchesFatYouHold()
	{
		RuneTickApi.FatSpread fat = new RuneTickApi.FatSpread();
		fat.setId(4151);
		fat.setName("Abyssal whip");
		fat.setBid(1_000_000);
		fat.setAsk(1_200_000);
		fat.setMarginPerUnit(160_000);
		fat.setDreamGp(5_000_000);

		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(4151, 3);

		List<BoardLine> hits = BankHits.build(
			bank,
			Collections.singletonList(fat),
			Collections.emptyList(),
			Collections.emptyList(),
			1_000_000,
			50,
			false,
			8);

		assertEquals(1, hits.size());
		assertEquals(4151, hits.get(0).itemId);
		assertTrue(BoardRow.total(hits.get(0).row).contains("have 3"));
	}

	@Test
	public void skipsBelowProfitFloor()
	{
		RuneTickApi.FatSpread fat = new RuneTickApi.FatSpread();
		fat.setId(1);
		fat.setName("Cheap");
		fat.setBid(10);
		fat.setAsk(20);
		fat.setMarginPerUnit(5);
		fat.setDreamGp(100);

		Map<Integer, Integer> bank = Collections.singletonMap(1, 50);
		List<BoardLine> hits = BankHits.build(
			bank,
			Collections.singletonList(fat),
			Collections.emptyList(),
			Collections.emptyList(),
			1_000_000,
			50,
			false,
			8);
		assertTrue(hits.isEmpty());
	}
}
