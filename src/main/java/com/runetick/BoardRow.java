package com.runetick;

/**
 * One board line: {@code name | buy->sell | total} (ASCII, font-safe).
 */
final class BoardRow
{
	private BoardRow()
	{
	}

	static String format(String name, String mid, String total)
	{
		String n = HudText.ascii(name == null ? "?" : name);
		String m = HudText.ascii(mid == null ? "" : mid);
		String t = HudText.ascii(total == null ? "" : total);
		return n + "\t" + m + "\t" + t;
	}

	static String name(String row)
	{
		return part(row, 0);
	}

	static String mid(String row)
	{
		return part(row, 1);
	}

	static String total(String row)
	{
		return part(row, 2);
	}

	private static String part(String row, int idx)
	{
		if (row == null || row.isEmpty())
		{
			return "";
		}
		String[] bits = row.split("\t", 3);
		return idx < bits.length ? bits[idx] : "";
	}
}
