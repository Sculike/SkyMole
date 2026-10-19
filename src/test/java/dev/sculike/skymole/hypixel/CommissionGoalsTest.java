package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CommissionGoalsTest {
	@Test
	void knownGoals() {
		assertEquals(10, CommissionGoals.goal("Royal Mines Titanium", MiningIsland.DWARVEN_MINES));
		assertEquals(250, CommissionGoals.goal("Cliffside Veins Mithril", MiningIsland.DWARVEN_MINES));
		assertEquals(500, CommissionGoals.goal("2x Mithril Powder Collector", MiningIsland.DWARVEN_MINES));
		assertEquals(100, CommissionGoals.goal("Goblin Slayer", MiningIsland.DWARVEN_MINES));
		assertEquals(13, CommissionGoals.goal("Goblin Slayer", MiningIsland.CRYSTAL_HOLLOWS));
		assertEquals(1000, CommissionGoals.goal("Amber Gemstone Collector", MiningIsland.CRYSTAL_HOLLOWS));
		assertEquals(1500, CommissionGoals.goal("Tungsten Collector", MiningIsland.DWARVEN_MINES));
	}

	@Test
	void countFromPercent() {
		assertEquals("5/10", CommissionGoals.count("Royal Mines Titanium", MiningIsland.DWARVEN_MINES, 50f));
		assertEquals("0/20", CommissionGoals.count("Goblin Raid Slayer", MiningIsland.DWARVEN_MINES, 0f));
		assertNull(CommissionGoals.count("Some New Commission", MiningIsland.DWARVEN_MINES, 50f));
	}
}
