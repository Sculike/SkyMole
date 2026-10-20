package dev.sculike.skymole.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ParsersTest {
	@Test
	void bossBarEvent() {
		// Exactly as in the screenshot from Hypixel
		Events.Event e = Events.parse("EVENT RAFFLE ACTIVE IN FAR RESERVE for 02:05");
		assertEquals("RAFFLE", e.name());
		assertEquals("Far Reserve", e.where());
		assertEquals("02:05", e.timeLeft());

		Events.Event powder = Events.parse("PASSIVE EVENT 2X POWDER RUNNING FOR 14:32");
		assertEquals("2X POWDER", powder.name());
		assertEquals("14:32", powder.timeLeft());

		assertNull(Events.parse("Some other boss bar"));
	}

	@Test
	void prettyNames() {
		assertEquals("Goblin Raid", Events.pretty("GOBLIN RAID"));
		assertEquals("2x Powder", Events.pretty("2X POWDER"));
		// From the real chat line "CLIFFSIDE VEINS MITHRIL Commission Complete! ..."
		assertEquals("Cliffside Veins Mithril", Events.pretty("CLIFFSIDE VEINS MITHRIL"));
		assertEquals("Rampart's Quarry Titanium", Events.pretty("RAMPART'S QUARRY TITANIUM"));
	}

	@Test
	void timeLeftOnTheBossBar() {
		assertEquals(125, Events.secondsLeft("02:05"));
		assertEquals(20, Events.secondsLeft("00:20"));
		assertEquals(3725, Events.secondsLeft("1:02:05"));
		assertEquals(-1, Events.secondsLeft(""));
		assertEquals(-1, Events.secondsLeft("soon"));
	}

	@Test
	void fetchur() {
		assertEquals("20x Yellow Stained Glass", Fetchur.answer("[NPC] Fetchur: its yellow and see through"));
		assertNull(Fetchur.answer("[NPC] Fetchur: something new"));
	}

	@Test
	void numbers() {
		assertEquals(3000, Tools.parse("3,000"));
		assertEquals(3000, Tools.parse("3k"));
		assertEquals("96.2k", PowderTracker.compact(96_210));
		assertEquals("1.25M", PowderTracker.compact(1_250_000));
	}
}
