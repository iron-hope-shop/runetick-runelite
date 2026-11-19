package com.runetick;

/**
 * When net-worth session tracking is allowed to arm, and how delta is computed.
 */
final class SessionDelta
{
	private SessionDelta()
	{
	}

	/** Full picture: bag scanned this login and bank opened at least once. */
	static boolean ready(boolean inventorySeen, boolean bankSeen)
	{
		return inventorySeen && bankSeen;
	}

	static long delta(boolean armed, Long baseline, long totalNow)
	{
		if (!armed || baseline == null)
		{
			return 0;
		}
		return totalNow - baseline;
	}
}
