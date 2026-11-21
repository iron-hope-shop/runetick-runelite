package com.runetick;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class CostBasisTest
{
	@Test
	public void unsoldBuysKeepAWeightedPrice()
	{
		List<GeTrade> trades = parse(
			"[{\"b\":true,\"i\":2,\"q\":10,\"p\":100,\"t\":1},"
				+ "{\"b\":true,\"i\":2,\"q\":10,\"p\":300,\"t\":2},"
				+ "{\"b\":false,\"i\":2,\"q\":5,\"t\":3,\"p\":400},"
				+ "{\"b\":true,\"i\":9,\"q\":1,\"p\":5,\"t\":4}]");
		CostBasis.Open open = CostBasis.openBuys(trades, 2);
		assertEquals(15, open.qty);
		assertEquals((5L * 100 + 10L * 300) / 15, open.each);
		assertEquals(5L, CostBasis.openBuys(trades, 9).each);
	}

	@Test
	public void aLaterSellClearsTheBuy()
	{
		List<GeTrade> trades = parse(
			"[{\"b\":true,\"i\":2,\"q\":4,\"p\":100,\"t\":1},"
				+ "{\"b\":false,\"i\":2,\"q\":4,\"p\":200,\"t\":2}]");
		assertNull(CostBasis.openBuys(trades, 2));
	}

	private static List<GeTrade> parse(String json)
	{
		return new Gson().fromJson(json, new TypeToken<List<GeTrade>>()
		{
		}.getType());
	}
}
