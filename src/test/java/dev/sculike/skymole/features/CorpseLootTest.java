package dev.sculike.skymole.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CorpseLootTest {
	@Test
	void rewardLines() {
		CorpseLoot.Reward a = CorpseLoot.parse("Glacite Powder x1,050");
		assertEquals("Glacite Powder", a.item());
		assertEquals(1050, a.amount());

		CorpseLoot.Reward b = CorpseLoot.parse("+16 Enchanted Umber");
		assertEquals("Enchanted Umber", b.item());
		assertEquals(16, b.amount());

		CorpseLoot.Reward c = CorpseLoot.parse("Shattered Locket");
		assertEquals("Shattered Locket", c.item());
		assertEquals(1, c.amount());

		assertNull(CorpseLoot.parse("REWARDS"));
		assertNull(CorpseLoot.parse("[MVP+] Someone: nice drop"));
		assertNull(CorpseLoot.parse("▬▬▬▬"));
	}
}
