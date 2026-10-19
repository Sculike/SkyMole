package dev.sculike.skymole.hypixel;

import java.util.List;
import java.util.Map;

/**
 * Where to go for a commission: names of spots in markers.json (the nearest one wins),
 * plus an optional tip. Commissions you can do anywhere get no spots and a note instead.
 */
public final class CommissionTargets {
	/**
	 * @param spots candidate spot names, empty when there's no fixed place
	 * @param tunnels true = only Glacite Tunnels spots, false = only the Dwarven Mines above, null = either
	 */
	public record Target(List<String> spots, boolean emissary, String note, Boolean tunnels) {
		/** The first candidate, or null. */
		public String spot() {
			return spots.isEmpty() ? null : spots.get(0);
		}

		static Target mines(String spot) { return new Target(List.of(spot), false, "", false); }
		static Target mines(String spot, String note) { return new Target(List.of(spot), false, note, false); }
		static Target tunnels(String spot) { return new Target(List.of(spot), false, "", true); }
		static Target tunnels(String spot, String note) { return new Target(List.of(spot), false, note, true); }
		static Target hollows(String spot) { return new Target(List.of(spot), false, "", null); }
		static Target hollows(String spot, String note) { return new Target(List.of(spot), false, note, null); }
		static Target nowhere(String note) { return new Target(List.of(), false, note, null); }
	}

	public static final Target EMISSARY = new Target(List.of(), true, "Claim it at the Emissary", null);

	private static final List<String> MITHRIL_AREAS = List.of("Cliffside Veins", "Lava Springs", "Rampart's Quarry", "Royal Mines", "Upper Mines");

	/** Any Mithril in the Dwarven Mines (not the tunnels): every marked Mithril spot plus the Mithril areas. */
	private static Target anyMithril(String note) {
		List<String> spots = new java.util.ArrayList<>();
		spots.add("Mithril");
		spots.addAll(MITHRIL_AREAS);
		return new Target(List.copyOf(spots), false, note, false);
	}

	private static final Map<String, Target> DWARVEN = Map.ofEntries(
			Map.entry("Mithril Miner", anyMithril("Nearest Mithril")),
			Map.entry("Titanium Miner", anyMithril("Titanium spawns while mining Mithril")),
			Map.entry("2x Mithril Powder Collector", anyMithril("During the 2x Powder event")),
			Map.entry("Goblin Slayer", Target.mines("Goblin Burrows")),
			Map.entry("Golden Goblin Slayer", Target.mines("Goblin Burrows", "Golden Goblins spawn while mining")),
			Map.entry("Glacite Walker Slayer", Target.mines("Great Ice Wall")),
			Map.entry("Ice Walker Slayer", Target.mines("Great Ice Wall")),
			Map.entry("Treasure Hoarder Puncher", Target.mines("Treasure Hoarder's Pond")),
			Map.entry("Star Sentry Puncher", Target.mines("Star Cult")),
			Map.entry("Mines Slayer", Target.nowhere("Any mob in the Mines counts")),
			// Glacite Tunnels (part of the Dwarven Mines island)
			Map.entry("Aquamarine Collector", Target.tunnels("Aquamarine Gemstone")),
			Map.entry("Citrine Collector", Target.tunnels("Citrine Gemstone")),
			Map.entry("Onyx Collector", Target.tunnels("Onyx Gemstone")),
			Map.entry("Peridot Collector", Target.tunnels("Peridot Gemstone")),
			Map.entry("Tungsten Collector", Target.tunnels("Tungsten")),
			Map.entry("Umber Collector", Target.tunnels("Umber")),
			Map.entry("Glacite Collector", Target.tunnels("Glacite Tunnels", "Glacite is everywhere in the tunnels")),
			Map.entry("Mineshaft Explorer", Target.nowhere("Mineshafts spawn randomly while mining in the tunnels")),
			Map.entry("Corpse Looter", Target.nowhere("Corpses are inside Mineshafts")),
			Map.entry("Scrap Collector", Target.nowhere("Suspicious Scrap drops while mining in the tunnels")),
			Map.entry("Maniac Slayer", Target.nowhere("Maniacs are inside Mineshafts")));

	private static final Map<String, Target> HOLLOWS = Map.ofEntries(
			Map.entry("Goblin Slayer", Target.hollows("Goblin Holdout")),
			Map.entry("Automaton Slayer", Target.hollows("Precursor Remnants")),
			Map.entry("Sludge Slayer", Target.hollows("Jungle")),
			Map.entry("Thyst Slayer", Target.hollows("Jungle")),
			Map.entry("Team Treasurite Member Slayer", Target.hollows("Mithril Deposits")),
			Map.entry("Boss Corleone Slayer", Target.hollows("Mithril Deposits")),
			Map.entry("Yog Slayer", Target.hollows("Magma Fields")),
			Map.entry("Chest Looter", Target.nowhere("Treasure chests appear while mining anywhere")),
			Map.entry("Hard Stone Miner", Target.nowhere("Hard Stone is everywhere")));

	/** Crystal structures move per lobby, so these point at the right quadrant. */
	private static final Map<String, Target> CRYSTALS = Map.of(
			"Amethyst", Target.hollows("Jungle", "Look for the Jungle Temple"),
			"Jade", Target.hollows("Mithril Deposits", "Look for the Mines of Divan"),
			"Amber", Target.hollows("Goblin Holdout", "Look for the Goblin Queen's Den"),
			"Sapphire", Target.hollows("Precursor Remnants", "Look for the Lost Precursor City"),
			"Topaz", Target.hollows("Magma Fields", "Look for Khazad-dûm"));

	private CommissionTargets() {}

	/**
	 * @param eventArea where the current mining event is (from the boss bar), or "" if unknown
	 */
	public static Target forCommission(String name, MiningIsland island, boolean done, String eventArea) {
		if (done) return EMISSARY;

		if (island == MiningIsland.CRYSTAL_HOLLOWS) {
			if (name.endsWith(" Crystal Hunter")) {
				Target t = CRYSTALS.get(name.substring(0, name.length() - " Crystal Hunter".length()));
				if (t != null) return t;
			}
			if (name.endsWith(" Gemstone Collector")) return Target.nowhere("Gemstones are spread over the whole Hollows");
			Target t = HOLLOWS.get(name);
			return t != null ? t : Target.nowhere("No known spot for this commission yet");
		}

		// Event commissions go wherever the event is.
		if (name.equals("Goblin Raid") || name.equals("Goblin Raid Slayer") || name.equals("Raffle")
				|| name.equals("Lucky Raffle") || name.equals("First Event")) {
			return eventArea.isEmpty() ? Target.nowhere("Wait for the event to start") : Target.mines(eventArea);
		}
		for (String area : MITHRIL_AREAS) {
			if (name.startsWith(area + " ")) return Target.mines(area);
		}
		Target t = DWARVEN.get(name);
		return t != null ? t : Target.nowhere("No known spot for this commission yet");
	}
}
