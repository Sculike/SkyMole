package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommissionTargetsTest {
	private static String spot(String name, MiningIsland island) {
		return CommissionTargets.forCommission(name, island, false, "").spot();
	}

	@Test
	void dwarvenMines() {
		assertEquals("Royal Mines", spot("Royal Mines Titanium", MiningIsland.DWARVEN_MINES));
		assertEquals("Cliffside Veins", spot("Cliffside Veins Mithril", MiningIsland.DWARVEN_MINES));
		assertEquals("Goblin Burrows", spot("Goblin Slayer", MiningIsland.DWARVEN_MINES));
		assertEquals("Peridot Gemstone", spot("Peridot Collector", MiningIsland.DWARVEN_MINES));
		assertNull(spot("Mineshaft Explorer", MiningIsland.DWARVEN_MINES));
	}

	@Test
	void mithrilUsesMinesSpotsAndAreasNotTunnels() {
		var t = CommissionTargets.forCommission("Mithril Miner", MiningIsland.DWARVEN_MINES, false, "");
		assertTrue(t.spots().contains("Mithril"));
		assertTrue(t.spots().contains("Royal Mines"));
		assertEquals(Boolean.FALSE, t.tunnels());
		assertEquals(Boolean.TRUE,
				CommissionTargets.forCommission("Peridot Collector", MiningIsland.DWARVEN_MINES, false, "").tunnels());
	}

	@Test
	void eventsGoToTheEvent() {
		assertEquals("Far Reserve",
				CommissionTargets.forCommission("Raffle", MiningIsland.DWARVEN_MINES, false, "Far Reserve").spot());
		assertNull(CommissionTargets.forCommission("Goblin Raid", MiningIsland.DWARVEN_MINES, false, "").spot());
	}

	@Test
	void crystalHollows() {
		assertEquals("Goblin Holdout", spot("Goblin Slayer", MiningIsland.CRYSTAL_HOLLOWS));
		assertEquals("Jungle", spot("Amethyst Crystal Hunter", MiningIsland.CRYSTAL_HOLLOWS));
	}

	@Test
	void doneGoesToEmissary() {
		assertTrue(CommissionTargets.forCommission("Goblin Slayer", MiningIsland.DWARVEN_MINES, true, "").emissary());
	}
}
