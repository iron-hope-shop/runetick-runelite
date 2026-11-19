package com.runetick;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

public class RuneTickPluginFingerprintTest
{
	@Test
	public void fingerprintChangesWithTradesOrCashOrOffers()
	{
		GeTrade a = new Gson().fromJson("{\"b\":true,\"i\":1,\"q\":2,\"p\":3,\"t\":4}", GeTrade.class);
		String empty = RuneTickPlugin.fingerprint(Collections.emptyList(), 0, 0, false, null, null);
		String withTrade = RuneTickPlugin.fingerprint(Collections.singletonList(a), 0, 0, false, null, null);
		String withCash = RuneTickPlugin.fingerprint(Collections.singletonList(a), 100, 0, false, null, null);
		assertNotEquals(empty, withTrade);
		assertNotEquals(withTrade, withCash);
		assertEquals(withTrade, RuneTickPlugin.fingerprint(Arrays.asList(a), 0, 0, false, null, null));

		RuneTickApi.GeLiveOffer live = new RuneTickApi.GeLiveOffer();
		live.setSlot(0);
		live.setItemId(1);
		live.setState("BUYING");
		live.setFilled(2);
		live.setSpent(100);
		String withOffer = RuneTickPlugin.fingerprint(Collections.singletonList(a), 100, 0, false,
			Collections.singletonList(live), null);
		assertNotEquals(withCash, withOffer);

		RuneTickApi.NetWorthSnap nw = new RuneTickApi.NetWorthSnap();
		nw.setTotalGp(999);
		nw.setOffersGp(1);
		String withNw = RuneTickPlugin.fingerprint(Collections.singletonList(a), 100, 0, false,
			Collections.singletonList(live), nw);
		assertNotEquals(withOffer, withNw);
	}
}
