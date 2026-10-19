package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ForgeTextTest {
	@Test
	void durations() {
		assertEquals(0, ForgeText.millisLeft("Ready!"));
		assertEquals(0, ForgeText.millisLeft("READY"));
		assertEquals((1 * 3600 + 23 * 60 + 4) * 1000L, ForgeText.millisLeft("1h 23m 4s"));
		assertEquals((2 * 86400 + 5 * 3600) * 1000L, ForgeText.millisLeft("2d 5h"));
		assertEquals(-1, ForgeText.millisLeft("Empty"));
	}

	@Test
	void tabLines() {
		ForgeText.Slot s = ForgeText.tabLine("1) Refined Mithril: 1h 23m");
		assertEquals(1, s.number());
		assertEquals("Refined Mithril", s.item());
		assertEquals((3600 + 23 * 60) * 1000L, s.millisLeft());
		assertEquals(0, ForgeText.tabLine("2) Fuel Tank: Ready!").millisLeft());
		assertNull(ForgeText.tabLine("3) Empty"));
	}
}
