package dev.sculike.skymole.hypixel;

import java.util.Map;

/**
 * How many of something each commission asks for, so "45%" can be shown as "112/250".
 * Hypixel's tab list only shows a percentage. Numbers from the SkyBlock wiki (Oct 2026).
 */
public final class CommissionGoals {
	private static final Map<String, Integer> EXACT = Map.ofEntries(
			// Dwarven Mines
			Map.entry("Glacite Walker Slayer", 50),
			Map.entry("Ice Walker Slayer", 50),
			Map.entry("Mithril Miner", 350),
			Map.entry("Titanium Miner", 15),
			Map.entry("Treasure Hoarder Puncher", 10),
			Map.entry("Goblin Raid", 1),
			Map.entry("Goblin Raid Slayer", 20),
			Map.entry("Golden Goblin Slayer", 1),
			Map.entry("Raffle", 1),
			Map.entry("Lucky Raffle", 20),
			Map.entry("Star Sentry Puncher", 10),
			Map.entry("2x Mithril Powder Collector", 500),
			Map.entry("First Event", 1),
			Map.entry("Mines Slayer", 50),
			// Crystal Hollows
			Map.entry("Automaton Slayer", 13),
			Map.entry("Sludge Slayer", 25),
			Map.entry("Team Treasurite Member Slayer", 13),
			Map.entry("Yog Slayer", 13),
			Map.entry("Chest Looter", 3),
			Map.entry("Thyst Slayer", 5),
			Map.entry("Hard Stone Miner", 1000),
			Map.entry("Boss Corleone Slayer", 1),
			// Glacite Tunnels
			Map.entry("Mineshaft Explorer", 1),
			Map.entry("Corpse Looter", 2),
			Map.entry("Scrap Collector", 1),
			Map.entry("Maniac Slayer", 10));

	private CommissionGoals() {}

	/** The goal for a commission, or 0 if unknown. */
	public static int goal(String name, MiningIsland island) {
		// Same name, different goal: Goblin Slayer is 100 in the Mines, 13 in the Hollows.
		if (name.equals("Goblin Slayer")) return island == MiningIsland.CRYSTAL_HOLLOWS ? 13 : 100;

		Integer exact = EXACT.get(name);
		if (exact != null) return exact;

		if (name.endsWith(" Gemstone Collector")) return 1000;   // Crystal Hollows
		if (name.endsWith(" Crystal Hunter")) return 1;
		if (name.endsWith(" Collector")) return 1500;            // Glacite: Onyx Collector, Tungsten Collector, ...
		if (name.endsWith(" Mithril")) return 250;               // Royal Mines Mithril, ...
		if (name.endsWith(" Titanium")) return 10;               // Royal Mines Titanium, ...
		return 0;
	}

	/** "112/250", or null if the goal is unknown. */
	public static String count(String name, MiningIsland island, float percent) {
		int goal = goal(name, island);
		if (goal <= 0) return null;
		int done = Math.min(goal, Math.round(percent / 100f * goal));
		return done + "/" + goal;
	}
}
