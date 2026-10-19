package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hud.HudElement;
import dev.sculike.skymole.hud.HudPanel;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.SkyBlockState;

/**
 * Powder gained this session (from the tab list's Powders widget), per hour,
 * plus pristine procs and treasure chests (from chat).
 */
public final class PowderTracker {
	private static final String[] TYPES = {"Mithril", "Gemstone", "Glacite"};
	private static final int[] COLORS = {Branding.MITHRIL, Branding.SNOUT_PINK, 0xFF9FD8FF};
	private static final Pattern PRISTINE = Pattern.compile("^PRISTINE! You found a Flawed (\\w+) Gemstone x(\\d+)!");
	private static final String CHEST = "You uncovered a treasure chest!";

	private static final Map<String, Long> start = new HashMap<>();
	private static final Map<String, Long> last = new HashMap<>();
	private static long activeMillis;
	private static long lastTick;
	private static long lastGain;
	private static int pristineProcs;
	private static long flawedGems;
	private static int chests;

	private PowderTracker() {}

	public static void register() {
		Chat.listen(text -> {
			Matcher m = PRISTINE.matcher(text);
			if (m.find()) {
				pristineProcs++;
				flawedGems += Long.parseLong(m.group(2));
			} else if (text.startsWith(CHEST)) {
				chests++;
			}
		});
	}

	public static void reset() {
		start.clear();
		last.clear();
		activeMillis = 0;
		lastGain = 0;
		pristineProcs = 0;
		flawedGems = 0;
		chests = 0;
	}

	/** Called after every state update. */
	public static void tick() {
		long now = System.currentTimeMillis();
		Map<String, Long> powders = SkyBlockState.powders();
		boolean gained = false;
		for (Map.Entry<String, Long> e : powders.entrySet()) {
			String type = e.getKey();
			long value = e.getValue();
			start.putIfAbsent(type, value);
			Long previous = last.put(type, value);
			if (previous == null) continue;
			if (value > previous) gained = true;
			// Spending powder (HOTM) shouldn't count as negative progress.
			if (value < previous) start.put(type, start.get(type) - (previous - value));
		}
		if (gained) lastGain = now;

		int pauseMinutes = SkyMoleConfig.get().powder.pauseAfterMinutes;
		boolean active = lastGain > 0 && (pauseMinutes == 0 || now - lastGain < pauseMinutes * 60_000L);
		if (active && lastTick > 0) activeMillis += Math.min(now - lastTick, 5_000);
		lastTick = now;
	}

	public static HudElement hud() {
		return new HudElement("Powder", () -> SkyMoleConfig.get().powder.hud, PowderTracker::title,
				PowderTracker::rows, PowderTracker::previewRows);
	}

	private static String title() {
		return Events.isActive("2X POWDER") ? "Powder  2x!" : "Powder";
	}

	private static List<HudPanel.Row> rows() {
		if (!SkyBlockState.onMiningIsland() || SkyBlockState.powders().isEmpty()) return null;
		SkyMoleConfig.Powder config = SkyMoleConfig.get().powder;
		List<HudPanel.Row> rows = new ArrayList<>();
		double hours = activeMillis / 3_600_000.0;
		for (int i = 0; i < TYPES.length; i++) {
			String type = TYPES[i];
			if (!enabled(config, i) || !last.containsKey(type)) continue;
			long gained = last.get(type) - start.getOrDefault(type, last.get(type));
			rows.add(row(type, gained, hours, config.rates, COLORS[i]));
		}
		addCounters(rows, config, pristineProcs, flawedGems, chests);
		rows.add(new HudPanel.Row("Time", formatTime(activeMillis), Branding.TEXT_DIM, -1));
		return rows;
	}

	private static List<HudPanel.Row> previewRows() {
		SkyMoleConfig.Powder config = SkyMoleConfig.get().powder;
		List<HudPanel.Row> rows = new ArrayList<>();
		long[] example = {48_210, 12_900, 3_150};
		for (int i = 0; i < TYPES.length; i++) {
			if (enabled(config, i)) rows.add(row(TYPES[i], example[i], 0.5, config.rates, COLORS[i]));
		}
		addCounters(rows, config, 14, 31, 22);
		rows.add(new HudPanel.Row("Time", "30m 0s", Branding.TEXT_DIM, -1));
		return rows;
	}

	private static void addCounters(List<HudPanel.Row> rows, SkyMoleConfig.Powder config, int procs, long gems, int chestCount) {
		if (config.pristine && procs > 0) rows.add(new HudPanel.Row("Pristine", procs + " (" + gems + " flawed)", Branding.SNOUT_PINK, -1));
		if (config.chests && chestCount > 0) rows.add(new HudPanel.Row("Treasure chests", String.valueOf(chestCount), Branding.LAMP_GOLD, -1));
	}

	private static boolean enabled(SkyMoleConfig.Powder config, int type) {
		return switch (type) {
			case 0 -> config.mithril;
			case 1 -> config.gemstone;
			default -> config.glacite;
		};
	}

	private static HudPanel.Row row(String type, long gained, double hours, boolean rates, int color) {
		String value = "+" + format(gained);
		if (rates && hours > 0.01) value += "  " + compact(gained / hours) + "/h";
		return new HudPanel.Row(type, value, color, -1);
	}

	static String format(long n) {
		return String.format(Locale.ROOT, "%,d", n);
	}

	/** 96210 -> "96.2k", 1250000 -> "1.25M". */
	static String compact(double n) {
		if (n >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", n / 1_000_000);
		if (n >= 1_000) return String.format(Locale.ROOT, "%.1fk", n / 1_000);
		return String.valueOf(Math.round(n));
	}

	static String formatTime(long millis) {
		long s = millis / 1000;
		if (s >= 3600) return (s / 3600) + "h " + (s % 3600 / 60) + "m";
		return (s / 60) + "m " + (s % 60) + "s";
	}
}
