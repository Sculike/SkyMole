package dev.sculike.skymole.hypixel;

import java.util.Map;

/**
 * Which Minecraft blocks are which SkyBlock ore, by block id. Blocks of the same ore
 * type are what Mining Spread can break together.
 */
public final class Ores {
	private static final Map<String, String> BY_ID = Map.ofEntries(
			// Mithril (all tiers count as Mithril)
			Map.entry("gray_wool", "Mithril"),
			Map.entry("cyan_terracotta", "Mithril"),
			Map.entry("prismarine", "Mithril"),
			Map.entry("prismarine_bricks", "Mithril"),
			Map.entry("dark_prismarine", "Mithril"),
			Map.entry("light_blue_wool", "Mithril"),
			Map.entry("polished_diorite", "Titanium"),
			// Glacite Tunnels
			Map.entry("packed_ice", "Glacite"),
			Map.entry("clay", "Tungsten"),
			Map.entry("cobblestone", "Tungsten"),
			Map.entry("terracotta", "Umber"),
			Map.entry("brown_terracotta", "Umber"),
			Map.entry("smooth_red_sandstone", "Umber"));

	/** Gemstones are stained glass (blocks and panes); the color decides the gem. */
	private static final Map<String, String> GEMS = Map.ofEntries(
			Map.entry("red", "Ruby"),
			Map.entry("orange", "Amber"),
			Map.entry("light_blue", "Sapphire"),
			Map.entry("lime", "Jade"),
			Map.entry("purple", "Amethyst"),
			Map.entry("yellow", "Topaz"),
			Map.entry("magenta", "Jasper"),
			Map.entry("white", "Opal"),
			Map.entry("blue", "Aquamarine"),
			Map.entry("brown", "Citrine"),
			Map.entry("black", "Onyx"),
			Map.entry("green", "Peridot"));

	private Ores() {}

	/** True for gemstone ore types (they use Gemstone Spread instead of Mining Spread). */
	public static boolean isGemstone(String type) {
		return GEMS.containsValue(type);
	}

	/** The ore type for a block id like "gray_wool" or "red_stained_glass_pane", or null. */
	public static String typeOf(String blockId) {
		String direct = BY_ID.get(blockId);
		if (direct != null) return direct;
		String color = null;
		if (blockId.endsWith("_stained_glass_pane")) color = blockId.substring(0, blockId.length() - "_stained_glass_pane".length());
		else if (blockId.endsWith("_stained_glass")) color = blockId.substring(0, blockId.length() - "_stained_glass".length());
		return color == null ? null : GEMS.get(color);
	}
}
