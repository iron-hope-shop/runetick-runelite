package com.runetick;

import java.awt.Color;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class AccentTest
{
	@Test
	public void parsesSiteHighlight()
	{
		assertEquals(new Color(0xfb, 0x92, 0x3c), Accent.parse("#fb923c"));
		assertEquals(new Color(0x22, 0xc5, 0x5e), Accent.parse("  #22C55E"));
	}

	@Test
	public void rejectsAnythingElse()
	{
		assertNull(Accent.parse(null));
		assertNull(Accent.parse("green"));
		assertNull(Accent.parse("#fff"));
		assertNull(Accent.parse("#22c55g"));
	}
}
