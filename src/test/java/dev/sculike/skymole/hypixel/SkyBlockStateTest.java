package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SkyBlockStateTest {
	@Test
	void readsAreaCommissionsAndPowders() {
		SkyBlockState.parseTabList(List.of(
				"         Players (24)",
				"Area: Dwarven Mines",
				" Server: mini123AB",
				"",
				"Commissions:",
				" Goblin Slayer: 45%",
				" Mithril Miner: 12.5%",
				" Lava Springs Titanium: DONE",
				"",
				"Powders:",
				" Mithril: 1,234,567",
				" Gemstone: 89,000",
				" Glacite: 0",
				"Skills: Mining 60"));

		assertEquals("Dwarven Mines", SkyBlockState.area());
		assertEquals(MiningIsland.DWARVEN_MINES, SkyBlockState.island());
		assertTrue(SkyBlockState.commissionWidgetFound());

		var commissions = SkyBlockState.commissions();
		assertEquals(3, commissions.size());
		assertEquals("Goblin Slayer", commissions.get(0).name());
		assertEquals(45f, commissions.get(0).percent());
		assertEquals(12.5f, commissions.get(1).percent());
		assertTrue(commissions.get(2).done());

		assertEquals(1_234_567L, SkyBlockState.powders().get("Mithril"));
		assertEquals(0L, SkyBlockState.powders().get("Glacite"));
	}

	@Test
	void notAMiningIslandAndNoWidget() {
		SkyBlockState.parseTabList(List.of("Area: Hub", "Commissions are fun"));
		assertEquals("Hub", SkyBlockState.area());
		assertEquals(null, SkyBlockState.island());
		assertFalse(SkyBlockState.commissionWidgetFound());
		assertTrue(SkyBlockState.commissions().isEmpty());
	}

	@Test
	void crystalHollowsCounts() {
		SkyBlockState.parseTabList(List.of("Area: Crystal Hollows"));
		assertEquals(MiningIsland.CRYSTAL_HOLLOWS, SkyBlockState.island());
	}
}
