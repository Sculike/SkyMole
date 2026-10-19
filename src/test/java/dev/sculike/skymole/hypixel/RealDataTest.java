package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Lines copied from a real /skymole debug file (Dwarven Mines, Oct 2026). */
class RealDataTest {
	@Test
	void scoreboardJunkCodesAreRemoved() {
		assertEquals("  Upper Mines", TabList.clean("  Upper Min§qes"));
		assertEquals("Purse: 15,698,538", TabList.clean("Purse: 15,698,§j538"));
		assertEquals("www.hypixel.net", TabList.clean("www.hypixel.ne§gt"));
	}

	@Test
	void zoneWithoutSymbolIsFound() {
		List<String> sidebar = List.of("10/19/26 m6M", "  ", " Early Spring 29th", " 1:20am", "  Upper Mines", "      ",
				"Purse: 15,698,538", "Bits: 20,719", "www.hypixel.net");
		SkyBlockState.parseSidebar(sidebar);
		assertEquals("Upper Mines", SkyBlockState.zone());
	}

	@Test
	void zoneWithSymbolStillWorks() {
		SkyBlockState.parseSidebar(List.of(" ⏣ Royal Mines", "Cold: -42❄"));
		assertEquals("Royal Mines", SkyBlockState.zone());
		assertEquals(42, SkyBlockState.cold());
	}

	@Test
	void realTabWidgets() {
		SkyBlockState.parseTabList(List.of(
				"               Info", "Area: Dwarven Mines", " Server: mini6M", "",
				"Commissions:", " Lucky Raffle: 0%", " Royal Mines Titanium: 0%", " Goblin Raid: 0%", " Raffle: 0%", "",
				"Powders:", " Mithril: 16,676", " Gemstone: 413", " Glacite: 0", "",
				"Pickaxe Ability:", " Pickobulus: 27s", "",
				"Stats:", " Speed: 400", " Strength: 231", " Crit Chance: 86", " Crit Damage: 183"));
		assertEquals(4, SkyBlockState.commissions().size());
		assertEquals(16_676L, SkyBlockState.powders().get("Mithril"));
		assertEquals(List.of("Pickobulus: 27s"), SkyBlockState.section("Pickaxe Ability"));
		// This Stats widget has no Mining Spread: that's what triggers the /widget popup.
		assertTrue(SkyBlockState.section("Stats").stream().noneMatch(l -> l.startsWith("Mining Spread")));
		assertFalse(SkyBlockState.section("Stats").isEmpty());
	}
}
