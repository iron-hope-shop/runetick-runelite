package com.runetick;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DepositToBankTest
{
	@Test
	public void removedIsWhatLeftTheBag()
	{
		Map<Integer, Integer> before = new HashMap<>();
		before.put(4151, 1);
		before.put(453, 100);
		Map<Integer, Integer> after = new HashMap<>();
		after.put(453, 40);
		Map<Integer, Integer> gone = DepositToBank.removed(before, after);
		assertEquals(Integer.valueOf(1), gone.get(4151));
		assertEquals(Integer.valueOf(60), gone.get(453));
	}

	@Test
	public void staleBankStillNeedsTheDeposit()
	{
		assertEquals(5, DepositToBank.stillMissing(10, 10, 5));
		assertEquals(5, DepositToBank.stillMissing(10, -1, 5));
	}

	@Test
	public void bankContainerAlreadyHoldingTheDepositIsNotAddedAgain()
	{
		assertEquals(0, DepositToBank.stillMissing(10, 15, 5));
	}
}
