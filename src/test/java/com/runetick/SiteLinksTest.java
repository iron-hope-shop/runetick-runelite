package com.runetick;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SiteLinksTest
{
	@Test
	public void buildsItemPath()
	{
		assertEquals(
			"https://runetick.ironhopeshop.com/item/4151",
			SiteLinks.itemUrl("https://runetick.ironhopeshop.com/", 4151));
	}

	@Test
	public void fallsBackToSiteRootWhenIdMissing()
	{
		assertEquals(
			RuneTickConfig.DEFAULT_SITE_URL,
			SiteLinks.itemUrl("", 0));
	}
}
