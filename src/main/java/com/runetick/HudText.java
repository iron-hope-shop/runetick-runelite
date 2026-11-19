package com.runetick;

/**
 * RuneLite's game font only has a small glyph set. Fancy Unicode (braille drag tabs,
 * ellipsis, arrows, middle dots, minus signs) renders as colored tofu squares in overlays.
 * Keep HUD / tip / sidebar copy in plain ASCII.
 */
final class HudText
{
	private HudText()
	{
	}

	/** Strip / replace characters the Runescape font cannot draw. */
	static String ascii(String s)
	{
		if (s == null || s.isEmpty())
		{
			return "";
		}
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); )
		{
			int cp = s.codePointAt(i);
			i += Character.charCount(cp);
			switch (cp)
			{
				case 0x2800: // ⠀..⠿ braille block often used as drag handles
				case 0x28FF:
					break;
				default:
					if (cp >= 0x2800 && cp <= 0x28FF)
					{
						break;
					}
					if (cp == 0x2026 || cp == 0x22EF) // … ⋯
					{
						out.append("...");
					}
					else if (cp == 0x00B7 || cp == 0x2022 || cp == 0x2219 || cp == 0x22C5) // · • ∙ ⋅
					{
						out.append(" | ");
					}
					else if (cp == 0x2192 || cp == 0x21D2 || cp == 0x2794) // → ⇒
					{
						out.append("->");
					}
					else if (cp == 0x2190) // ←
					{
						out.append("<-");
					}
					else if (cp == 0x2013 || cp == 0x2014 || cp == 0x2212) // – — −
					{
						out.append('-');
					}
					else if (cp == 0x00D7 || cp == 0x2715) // × ✕
					{
						out.append('x');
					}
					else if (cp == 0x2264) // ≤
					{
						out.append("<=");
					}
					else if (cp == 0x2265) // ≥
					{
						out.append(">=");
					}
					else if (cp == 0x2019 || cp == 0x2018) // ’ ‘
					{
						out.append('\'');
					}
					else if (cp == 0x201C || cp == 0x201D) // “ ”
					{
						out.append('"');
					}
					else if (cp < 32)
					{
						out.append(' ');
					}
					else if (cp <= 0x7E || Character.isLetterOrDigit(cp) || Character.isSpaceChar(cp))
					{
						out.appendCodePoint(cp);
					}
					else if (cp == ',' || cp == '.' || cp == '/' || cp == '%' || cp == ':' || cp == '+'
						|| cp == '-' || cp == '(' || cp == ')' || cp == '#' || cp == '@' || cp == '='
						|| cp == '?' || cp == '!' || cp == '&' || cp == '\'' || cp == '"' || cp == '|')
					{
						out.appendCodePoint(cp);
					}
					// else drop unsupported glyph
					break;
			}
		}
		return out.toString().replace("  ", " ").trim();
	}
}
