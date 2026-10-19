package dev.sculike.skymole.hypixel;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.features.Alerts;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Your Mining Spread and Gemstone Spread. The tab list's Stats widget doesn't always offer them,
 * so they're also read from any menu that shows them (SkyBlock Menu, Your Stats) and remembered.
 * Order: tab list, then the last value seen in a menu, then the manual value from the settings.
 */
public final class SpreadStats {
	private static final Pattern SPREAD = Pattern.compile("(Mining|Gemstone) Spread\\D*?([\\d,]+(?:\\.\\d+)?)");

	private SpreadStats() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container) || !isStatsMenu(screen.getTitle())) return;
			// Menus fill in a moment after opening, so look every tick while it's open.
			ScreenEvents.afterTick(screen).register(s -> {
				for (Slot slot : container.getMenu().slots) {
					// Your own items (a drill's "+10 Mining Spread") are bonuses, not your total: skip them.
					if (slot.container instanceof Inventory) continue;
					read(slot.getItem());
				}
			});
		});
	}

	/** The SkyBlock Menu and the stats/profile menus show your total stats. */
	static boolean isStatsMenu(Component title) {
		String t = TabList.clean(title.getString()).toLowerCase(Locale.ROOT);
		return t.contains("skyblock menu") || t.contains("profile") || t.contains("stats");
	}

	private static void read(ItemStack stack) {
		if (stack.isEmpty()) return;
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) return;
		for (Component line : lore.lines()) {
			Matcher m = SPREAD.matcher(TabList.clean(line.getString()));
			if (!m.find()) continue;
			double value = Double.parseDouble(m.group(2).replace(",", ""));
			remember(m.group(1).toLowerCase(Locale.ROOT).equals("gemstone"), value);
		}
	}

	private static void remember(boolean gemstone, double value) {
		SkyMoleConfig.Tools t = SkyMoleConfig.get().tools;
		double old = gemstone ? t.gemstoneSpreadSeen : t.miningSpreadSeen;
		if (old == value) return;
		if (gemstone) t.gemstoneSpreadSeen = value;
		else t.miningSpreadSeen = value;
		SkyMoleConfig.save();
		if (old < 0) Alerts.chat((gemstone ? "Gemstone" : "Mining") + " Spread saved: " + format(value));
	}

	/** From the tab list's Stats widget, or -1. */
	private static double fromTab(String name) {
		for (var section : SkyBlockState.sections().values()) {
			for (String line : section) {
				if (!line.startsWith(name + ":")) continue;
				String digits = line.substring(name.length() + 1).replaceAll("[^0-9.]", "");
				try {
					return digits.isEmpty() ? -1 : Double.parseDouble(digits);
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		}
		return -1;
	}

	/**
	 * Spread for an ore type: Gemstone Spread for gemstones, Mining Spread for everything else.
	 * Returns -1 when it isn't known at all.
	 */
	public static double forOre(String ore) {
		boolean gem = ore != null && Ores.isGemstone(ore);
		String name = gem ? "Gemstone Spread" : "Mining Spread";
		double tab = fromTab(name);
		if (tab >= 0) return tab;
		SkyMoleConfig.Tools t = SkyMoleConfig.get().tools;
		double seen = gem ? t.gemstoneSpreadSeen : t.miningSpreadSeen;
		if (seen >= 0) return seen;
		return t.manualSpread > 0 ? t.manualSpread : -1;
	}

	private static String format(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
	}
}
