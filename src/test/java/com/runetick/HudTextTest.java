package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class HudTextTest
{
	@Test
	public void stripsUnsupportedOverlayGlyphs()
	{
		assertEquals("Runetick", HudText.ascii("⠿ Runetick"));
		assertEquals("1,000->1,200 | usual 1,500", HudText.ascii("1,000→1,200 · usual 1,500"));
		assertEquals("loading...", HudText.ascii("loading…"));
		assertEquals("-49%", HudText.ascii("−49%"));
		assertEquals("buy <= 10 -> sell 12", HudText.ascii("buy ≤ 10 → sell 12"));
		assertFalse(HudText.ascii("⠿").contains("\u280F"));
	}
}
