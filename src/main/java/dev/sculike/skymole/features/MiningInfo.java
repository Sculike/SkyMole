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
import dev.sculike.skymole.hypixel.MiningIsland;
import dev.sculike.skymole.hypixel.SkyBlockState;
import net.minecraft.client.Minecraft;

/**
 * The "Mining info" panel: zone, Glacite cold, Crystal Hollows crystals and lobby day,
 * Sky Mall perk and the Scatha worm cooldown. Also the worm and Mineshaft alerts.
 */
public final class MiningInfo {
	private static final Pattern SKY_MALL = Pattern.compile("^New buff: (.+)$");
	private static final String WORM = "You hear the sound of something approaching...";
	private static final long WORM_COOLDOWN_MS = 30_000;

	private static String skyMall = "";
	private static long wormAt;
	private static boolean coldWarned;

	private MiningInfo() {}

	public static void register() {
		Chat.listen(text -> {
			SkyMoleConfig.Area config = SkyMoleConfig.get().area;
			Matcher mall = SKY_MALL.matcher(text);
			if (mall.find()) {
				skyMall = mall.group(1).trim();
			} else if (text.startsWith(WORM)) {
				wormAt = System.currentTimeMillis();
				if (config.wormAlert) Alerts.show("Worm incoming!", "");
			} else if (text.toLowerCase(Locale.ROOT).contains("glacite mineshaft portal") && config.mineshaftAlert) {
				Alerts.show("Mineshaft found!", "");
			}
		});
	}

	/** Called after every state update. */
	public static void tick() {
		SkyMoleConfig.Area config = SkyMoleConfig.get().area;
		boolean cold = config.coldWarning > 0 && SkyBlockState.cold() >= config.coldWarning;
		if (cold && !coldWarned) Alerts.show("You're freezing!", "Cold " + SkyBlockState.cold() + " / 100");
		coldWarned = cold;
	}

	public static HudElement hud() {
		return new HudElement("Mining info", () -> SkyMoleConfig.get().area.hud, () -> "Mining info",
				MiningInfo::rows,
				() -> List.of(
						new HudPanel.Row("Zone", "Royal Mines", Branding.TEXT_DIM, -1),
						new HudPanel.Row("Cold", "42", 0xFF9FD8FF, 0.42f),
						new HudPanel.Row("Sky Mall", "+100 Mining Speed", Branding.LAMP_GOLD, -1),
						new HudPanel.Row("Crystals", "3/5", Branding.SNOUT_PINK, 0.6f),
						new HudPanel.Row("Lobby day", "4", Branding.TEXT_DIM, -1),
						new HudPanel.Row("Worm", "Ready", Branding.GOOD, -1)));
	}

	private static List<HudPanel.Row> rows() {
		MiningIsland island = SkyBlockState.island();
		if (island == null) return null;
		SkyMoleConfig.Area config = SkyMoleConfig.get().area;
		List<HudPanel.Row> rows = new ArrayList<>();

		if (config.zone && !SkyBlockState.zone().isEmpty()) {
			rows.add(new HudPanel.Row("Zone", SkyBlockState.zone(), Branding.TEXT_DIM, -1));
		}
		if (config.cold && SkyBlockState.cold() > 0) {
			int cold = SkyBlockState.cold();
			int color = config.coldWarning > 0 && cold >= config.coldWarning ? Branding.BAD : 0xFF9FD8FF;
			rows.add(new HudPanel.Row("Cold", String.valueOf(cold), color, cold / 100f));
		}
		if (config.skyMall && !skyMall.isEmpty()
				&& (island == MiningIsland.DWARVEN_MINES || island == MiningIsland.CRYSTAL_HOLLOWS)) {
			rows.add(new HudPanel.Row("Sky Mall", skyMall, Branding.LAMP_GOLD, -1));
		}
		if (island == MiningIsland.CRYSTAL_HOLLOWS) {
			if (config.crystals) addCrystals(rows);
			if (config.lobbyDay) {
				var level = Minecraft.getInstance().level;
				if (level != null) {
					long day = level.getOverworldClockTime() / 24000L;
					rows.add(new HudPanel.Row("Lobby day", String.valueOf(day), Branding.TEXT_DIM, -1));
				}
			}
			if (config.wormCooldown && wormAt > 0) {
				long left = WORM_COOLDOWN_MS - (System.currentTimeMillis() - wormAt);
				rows.add(left > 0
						? new HudPanel.Row("Worm", (left / 1000 + 1) + "s", Branding.WARN, 1 - left / (float) WORM_COOLDOWN_MS)
						: new HudPanel.Row("Worm", "Ready", Branding.GOOD, -1));
			}
		}
		return rows.isEmpty() ? null : rows;
	}

	/** The tab list "Crystals" widget lists each crystal as Found / Not Found / Placed. */
	private static void addCrystals(List<HudPanel.Row> rows) {
		List<String> lines = SkyBlockState.section("Crystals");
		if (lines.isEmpty()) return;
		int found = 0;
		int total = 0;
		for (String line : lines) {
			if (!line.contains(":")) continue;
			total++;
			String state = line.substring(line.indexOf(':') + 1).toLowerCase(Locale.ROOT);
			if (!state.contains("not found")) found++;
		}
		if (total > 0) rows.add(new HudPanel.Row("Crystals", found + "/" + total, Branding.SNOUT_PINK, found / (float) total));
	}
}
