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

	@Test
	void orangeIsFreeOnlyAsOftenAsSpreadClearsTheWay() {
		// Already in the open: nothing has to break first.
		assertEquals(1.0, BestBlock.freeChance(3, 0, true), 1e-9);
		// 200 spread breaks about 2 of green's 4 neighbours, so a given one is even odds.
		assertEquals(0.5, BestBlock.freeChance(4, 200, false), 1e-9);
		// Enough spread to clear every neighbour: the way is always open.
		assertEquals(1.0, BestBlock.freeChance(2, 400, false), 1e-9);
		// No spread at all means nothing extra breaks.
		assertEquals(0.0, BestBlock.freeChance(4, 0, false), 1e-9);
		// Green with no neighbours cannot uncover anything.
		assertEquals(0.0, BestBlock.freeChance(0, 500, false), 1e-9);
	}

	@Test
	void expectedBlocksCapsAtTheNeighboursThatExist() {
		assertEquals(1.0, BestBlock.expectedBlocks(0, 300), 1e-9);
		assertEquals(3.0, BestBlock.expectedBlocks(2, 300), 1e-9);
		// 216 spread cannot break more than the 2 neighbours present.
		assertEquals(3.0, BestBlock.expectedBlocks(2, 216), 1e-9);
	}

	@Test
	void compactLineNamesTheItem() {
		java.util.regex.Pattern compact = java.util.regex.Pattern.compile("^COMPACT! You found an? (.+?)!");
		java.util.regex.Matcher m = compact.matcher("COMPACT! You found an Enchanted Diamond!");
		assertEquals(true, m.find());
		assertEquals("Enchanted Diamond", m.group(1));

		m = compact.matcher("COMPACT! You found a Enchanted Iron Ingot!");
		assertEquals(true, m.find());
		assertEquals("Enchanted Iron Ingot", m.group(1));

		assertEquals(false, compact.matcher("You found a diamond").find());
	}
}
