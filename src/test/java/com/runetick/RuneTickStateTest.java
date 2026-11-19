package com.runetick;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RuneTickStateTest
{
	@Test
	public void prefersOpenFlipAndBuildsHighlightLabels()
	{
		RuneTickState state = new RuneTickState();
		state.setLinked(true);

		RuneTickApi.PlacedFlip buying = new RuneTickApi.PlacedFlip();
		buying.setId("a");
		buying.setItemId(1);
		buying.setItemName("A");
		buying.setUnits(10);
		buying.setBought(2);
		buying.setBid(100);
		buying.setAsk(120);
		buying.setStatus("buying");

		RuneTickApi.PlacedFlip selling = new RuneTickApi.PlacedFlip();
		selling.setId("b");
		selling.setItemId(2);
		selling.setItemName("B");
		selling.setUnits(5);
		selling.setBought(5);
		selling.setSold(1);
		selling.setBid(50);
		selling.setAsk(80);
		selling.setStatus("selling");

		state.setPlaced(Arrays.asList(buying, selling), null);

		RuneTickApi.Plan plan = new RuneTickApi.Plan();
		RuneTickApi.PlanPosition rec = new RuneTickApi.PlanPosition();
		rec.setId(3);
		rec.setName("C");
		rec.setBid(200);
		rec.setAsk(220);
		plan.setPositions(Collections.singletonList(rec));
		plan.setPace("fast");
		plan.setExpectedGpPerHour(9000);
		state.setPlan(plan);

		assertEquals(buying, state.placedFor(1));
		assertEquals(rec, state.recommendationFor(3));
		assertNull(state.recommendationFor(99));
		assertEquals("fast", state.getPlanPace());

		Map<Integer, String> labels = state.highlightLabels();
		assertTrue(labels.get(1).startsWith("buy "));
		assertTrue(labels.get(2).startsWith("sell "));
		assertTrue(labels.get(3).startsWith("buy "));
	}
}
