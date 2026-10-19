package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hud.HudElement;
import dev.sculike.skymole.hud.HudPanel;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.TabList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/** Pickaxe ability cooldown, drill fuel and mining stats. */
public final class Tools {
	private static final Pattern USED = Pattern.compile("^You used your (.+?) Pickaxe Ability!");
	private static final Pattern READY = Pattern.compile("^(.+?) is now available!");
	private static final Pattern FUEL = Pattern.compile("Fuel: ([\\d,]+)/([\\d,.]+k?)");
	private static final String[] STAT_WORDS = {"Mining Speed", "Mining Fortune", "Pristine", "Mining Spread"};

	private static String abilityName = "";
	private static boolean abilityReady = true;
	private static long abilityUsedAt;
	private static boolean lowFuelWarned;

	private Tools() {}

	public static void register() {
		Chat.listen(text -> {
			Matcher used = USED.matcher(text);
			if (used.find()) {
				abilityName = used.group(1);
				abilityReady = false;
				abilityUsedAt = System.currentTimeMillis();
				return;
			}
			Matcher ready = READY.matcher(text);
			if (ready.find() && !abilityName.isEmpty() && ready.group(1).equalsIgnoreCase(abilityName)) {
				abilityReady = true;
				if (SkyMoleConfig.get().tools.abilityReadyAlert) Alerts.show(abilityName + " ready!", "");
			} else if (ready.find(0) && ready.group(1).equalsIgnoreCase("Pickobulus")) {
				// Ready message before we saw it being used (e.g. right after joining).
				abilityName = "Pickobulus";
				abilityReady = true;
			}
		});
	}

	/**
	 * True when this pickaxe ability is ready: from the tab list's Pickaxe Ability widget
	 * ("Pickobulus: Available"), otherwise from the chat messages.
	 */
	public static boolean isAbilityReady(String name) {
		for (String line : SkyBlockState.section("Pickaxe Ability")) {
			if (!line.regionMatches(true, 0, name, 0, name.length())) continue;
			return line.toLowerCase(java.util.Locale.ROOT).contains("available");
		}
		return abilityName.equalsIgnoreCase(name) && abilityReady;
	}

	/** True if you're holding a pickaxe, drill or gauntlet (SkyBlock names them like that). */
	public static boolean holdingMiningTool() {
		var player = Minecraft.getInstance().player;
		if (player == null) return false;
		String name = TabList.clean(player.getMainHandItem().getHoverName().getString()).toLowerCase(java.util.Locale.ROOT);
		return name.contains("pick") || name.contains("drill") || name.contains("gauntlet");
	}

	/** Drill fuel from the held item's lore: {current, max}, or null if it's not a drill. */
	static long[] heldFuel() {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) return null;
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) return null;
		for (Component line : lore.lines()) {
			Matcher m = FUEL.matcher(TabList.clean(line.getString()));
			if (m.find()) return new long[] {parse(m.group(1)), parse(m.group(2))};
		}
		return null;
	}

	/** "3,000" -> 3000, "3k" -> 3000. */
	static long parse(String number) {
		String n = number.replace(",", "").toLowerCase(Locale.ROOT);
		if (n.endsWith("k")) return Math.round(Double.parseDouble(n.substring(0, n.length() - 1)) * 1000);
		return Math.round(Double.parseDouble(n));
	}

	/** Called after every state update. */
	public static void tick() {
		SkyMoleConfig.Tools config = SkyMoleConfig.get().tools;
		long[] fuel = heldFuel();
		if (fuel == null || fuel[1] <= 0 || config.lowFuelPercent <= 0) return;
		boolean low = fuel[0] * 100 / fuel[1] < config.lowFuelPercent;
		if (low && !lowFuelWarned) Alerts.show("Low drill fuel!", fuel[0] + " left");
		lowFuelWarned = low;
	}

	public static HudElement hud() {
		return new HudElement("Tools", () -> SkyMoleConfig.get().tools.hud, () -> "Tools",
				Tools::rows,
				() -> List.of(
						new HudPanel.Row("Pickobulus", "Ready", Branding.GOOD, -1),
						new HudPanel.Row("Fuel", "2,450/3,000", Branding.TEXT_DIM, 0.82f),
						new HudPanel.Row("Mining Speed", "2,840", Branding.TEXT_DIM, -1),
						new HudPanel.Row("Mining Fortune", "412", Branding.TEXT_DIM, -1)));
	}

	private static List<HudPanel.Row> rows() {
		if (!SkyBlockState.onMiningIsland()) return null;
		SkyMoleConfig.Tools config = SkyMoleConfig.get().tools;
		List<HudPanel.Row> rows = new ArrayList<>();

		if (config.ability) {
			// The tab list's "Pickaxe Ability" widget is exact; chat is the fallback.
			List<String> tab = SkyBlockState.section("Pickaxe Ability");
			if (!tab.isEmpty()) {
				for (String line : tab) {
					int colon = line.indexOf(':');
					if (colon < 0) continue;
					String value = line.substring(colon + 1).trim();
					boolean ready = value.equalsIgnoreCase("Available");
					rows.add(new HudPanel.Row(line.substring(0, colon).trim(), ready ? "Ready" : value,
							ready ? Branding.GOOD : Branding.WARN, -1));
				}
			} else if (!abilityName.isEmpty()) {
				long seconds = (System.currentTimeMillis() - abilityUsedAt) / 1000;
				rows.add(new HudPanel.Row(abilityName, abilityReady ? "Ready" : "used " + seconds + "s ago",
						abilityReady ? Branding.GOOD : Branding.WARN, -1));
			}
		}

		if (config.fuel) {
			long[] fuel = heldFuel();
			if (fuel != null && fuel[1] > 0) {
				float f = fuel[0] / (float) fuel[1];
				int color = f * 100 < config.lowFuelPercent ? Branding.BAD : Branding.TEXT_DIM;
				rows.add(new HudPanel.Row("Fuel", PowderTracker.format(fuel[0]) + "/" + PowderTracker.format(fuel[1]), color, f));
			}
		}

		if (config.stats) {
			for (List<String> section : SkyBlockState.sections().values()) {
				for (String line : section) {
					for (String word : STAT_WORDS) {
						if (!line.startsWith(word + ":")) continue;
						rows.add(new HudPanel.Row(word, line.substring(word.length() + 1).trim(), Branding.TEXT_DIM, -1));
					}
				}
			}
		}
		ForgeReminder.addRows(rows);
		return rows.isEmpty() ? null : rows;
	}
}
