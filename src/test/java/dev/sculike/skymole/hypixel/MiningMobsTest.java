package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningMobsTest {
	@Test
	void nameTags() {
		assertEquals("Goblin", MiningMobs.match("[Lv6] Goblin 100/100❤").name());
		assertEquals("Golden Goblin", MiningMobs.match("[Lv50] Golden Goblin 200/200❤").name());
		assertEquals("Emerald Slime", MiningMobs.match("[Lv15] Emerald Slime 150/150❤").name());
		assertEquals("Yog", MiningMobs.match("[Lv100] Yog 30k❤").name());
		// Whole words only: "Yogurt" isn't a Yog, a player called "Goblinator" isn't a Goblin.
		assertNull(MiningMobs.match("Yogurt Seller"));
		assertNull(MiningMobs.match("Goblinator"));
	}

	@Test
	void forgeNpcs() {
		assertTrue(MiningMobs.isForgeNpc("Forge Foreman"));
		assertTrue(MiningMobs.isForgeNpc("Forger"));
		assertFalse(MiningMobs.isForgeNpc("CLICK"));
	}
}
