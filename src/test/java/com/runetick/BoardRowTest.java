package com.runetick;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BoardRowTest
{
	@Test
	public void formatsThreeColumns()
	{
		String row = BoardRow.format("Drake bones", "4,530->5,995", "10,095,000");
		assertEquals("Drake bones", BoardRow.name(row));
		assertEquals("4,530->5,995", BoardRow.mid(row));
		assertEquals("10,095,000", BoardRow.total(row));
	}
}
