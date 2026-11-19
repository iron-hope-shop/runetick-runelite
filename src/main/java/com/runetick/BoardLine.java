package com.runetick;

/**
 * One clickable board row: item id + {@link BoardRow} text.
 */
final class BoardLine
{
	final int itemId;
	final String row;

	BoardLine(int itemId, String row)
	{
		this.itemId = itemId;
		this.row = row == null ? "" : row;
	}

	static BoardLine of(int itemId, String name, String mid, String total)
	{
		return new BoardLine(itemId, BoardRow.format(name, mid, total));
	}
}
