package dev.sculike.skymole.hypixel;

/** The SkyBlock islands where you mine. Names match the tab list "Area:" line. */
public enum MiningIsland {
	DWARVEN_MINES("Dwarven Mines"),   // includes the Glacite Tunnels
	CRYSTAL_HOLLOWS("Crystal Hollows"),
	MINESHAFT("Mineshaft"),           // Glacite Mineshafts
	GOLD_MINE("Gold Mine"),
	DEEP_CAVERNS("Deep Caverns");

	public final String areaName;

	MiningIsland(String areaName) {
		this.areaName = areaName;
	}

	/** The mining island for a tab list area name, or null if it's not a mining island. */
	public static MiningIsland fromArea(String area) {
		for (MiningIsland island : values()) {
			if (island.areaName.equalsIgnoreCase(area)) return island;
		}
		return null;
	}

	/** Islands where your commissions show in the tab list (Mineshafts belong to the Glacite Tunnels). */
	public boolean hasCommissions() {
		return this == DWARVEN_MINES || this == CRYSTAL_HOLLOWS || this == MINESHAFT;
	}
}
