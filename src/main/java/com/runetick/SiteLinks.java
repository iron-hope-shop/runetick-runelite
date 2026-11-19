package com.runetick;

/**
 * Runetick website URLs.
 */
final class SiteLinks
{
	private SiteLinks()
	{
	}

	static String itemUrl(String siteBase, int itemId)
	{
		String base = siteBase == null || siteBase.trim().isEmpty()
			? RuneTickConfig.DEFAULT_SITE_URL
			: siteBase.trim();
		while (base.endsWith("/"))
		{
			base = base.substring(0, base.length() - 1);
		}
		if (itemId <= 0)
		{
			return base;
		}
		return base + "/item/" + itemId;
	}
}
