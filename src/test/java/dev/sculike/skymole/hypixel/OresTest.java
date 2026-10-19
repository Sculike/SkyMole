package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class OresTest {
	@Test
	void types() {
		assertEquals("Mithril", Ores.typeOf("light_blue_wool"));
		assertEquals("Mithril", Ores.typeOf("prismarine_bricks"));
		assertEquals("Titanium", Ores.typeOf("polished_diorite"));
		assertEquals("Ruby", Ores.typeOf("red_stained_glass"));
		assertEquals("Ruby", Ores.typeOf("red_stained_glass_pane"));
		// light_blue glass is Sapphire, light_blue wool is Mithril: the suffix decides.
		assertEquals("Sapphire", Ores.typeOf("light_blue_stained_glass"));
		assertNull(Ores.typeOf("stone"));
	}
}
