package com.runetick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SessionDeltaTest
{
	@Test
	public void waitsForInvAndBank()
	{
		assertFalse(SessionDelta.ready(false, false));
		assertFalse(SessionDelta.ready(true, false));
		assertFalse(SessionDelta.ready(false, true));
		assertTrue(SessionDelta.ready(true, true));
	}

	@Test
	public void deltaZeroUntilArmed()
	{
		assertEquals(0, SessionDelta.delta(false, 100L, 150L));
		assertEquals(0, SessionDelta.delta(true, null, 150L));
		assertEquals(50, SessionDelta.delta(true, 100L, 150L));
		assertEquals(-25, SessionDelta.delta(true, 100L, 75L));
	}
}
