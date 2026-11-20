package com.runetick;

import java.awt.Color;

/** Highlight color chosen on the site. Titles and marks use this. Prices do not. */
public final class Accent
{
	private static final Color FALLBACK = new Color(0x22, 0xc5, 0x5e);
	private static volatile Color color = FALLBACK;

	private Accent()
	{
	}

	public static Color color()
	{
		return color;
	}

	public static void set(String hex)
	{
		Color next = parse(hex);
		if (next != null)
		{
			color = next;
		}
	}

	static Color parse(String hex)
	{
		if (hex == null)
		{
			return null;
		}
		String s = hex.trim();
		if (s.length() != 7 || s.charAt(0) != '#')
		{
			return null;
		}
		try
		{
			int rgb = Integer.parseInt(s.substring(1), 16);
			return new Color(rgb);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
