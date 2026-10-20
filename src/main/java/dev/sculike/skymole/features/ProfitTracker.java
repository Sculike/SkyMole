package dev.sculike.skymole.features;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.config.SkyMoleConfig.Totals;
import dev.sculike.skymole.hud.HudElement;
import dev.sculike.skymole.hud.HudPanel;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.Prices;
import dev.sculike.skymole.hypixel.SkyBlockState;

/**
 * What this session, today and all time have brought in: powder exactly, and coins for the drops
 * Hypixel announces in chat (compact procs, pristine gemstones, corpse loot). Ores that go
 * straight into your sacks are only counted as "+58 items" by Hypixel, with no names, so they
 * can't be valued.
 */
public final class ProfitTracker {
	private static final Pattern COMPACT = Pattern.compile("^COMPACT! You found an? (.+?)!");
	private static final Pattern PRISTINE = Pattern.compile("^PRISTINE! You found a (Flawed \\w+ Gemstone) x([\\d,]+)!");

	private static final Totals session = new Totals();

	private ProfitTracker() {}

	public static void register() {
		Chat.listen(ProfitTracker::onChat);
	}

	/** Drops that Hypixel names in chat can be priced. */
	static void onChat(String text) {
		Matcher compact = COMPACT.matcher(text);
		if (compact.find()) {
			addCoins(Prices.of(compact.group(1)));
			return;
		}
		Matcher pristine = PRISTINE.matcher(text);
		if (pristine.find()) {
			double each = Prices.of(pristine.group(1));
			if (each > 0) addCoins(each * Long.parseLong(pristine.group(2).replace(",", "")));
		}
	}

	/** Keeps "today" tied to the actual day. */
	private static void rollOver() {
		SkyMoleConfig.Tracker t = SkyMoleConfig.get().tracker;
		String today = LocalDate.now().toString();
		if (!today.equals(t.todayDate)) {
			t.todayDate = today;
			t.today.clear();
			SkyMoleConfig.save();
		}
	}

	private static List<Totals> all() {
		SkyMoleConfig.Tracker t = SkyMoleConfig.get().tracker;
		rollOver();
		return List.of(session, t.today, t.allTime);
	}

	public static void addPowder(String type, long amount) {
		if (amount <= 0) return;
		for (Totals t : all()) {
			switch (type) {
				case "Mithril" -> t.mithril += amount;
				case "Gemstone" -> t.gemstone += amount;
				case "Glacite" -> t.glacite += amount;
				default -> { }
			}
		}
	}

	public static void addCoins(double amount) {
		if (amount <= 0) return;
		for (Totals t : all()) t.coins += amount;
	}

	/** Time spent actually mining, counted by the powder tracker. */
	public static void addActive(long millis) {
		if (millis <= 0) return;
		for (Totals t : all()) t.millis += millis;
	}

	public static void resetSession() {
		session.clear();
	}

	public static void reset(int scope) {
		SkyMoleConfig.Tracker t = SkyMoleConfig.get().tracker;
		switch (scope) {
			case 1 -> t.today.clear();
			case 2 -> t.allTime.clear();
			default -> session.clear();
		}
		SkyMoleConfig.save();
	}

	private static Totals current() {
		SkyMoleConfig.Tracker t = SkyMoleConfig.get().tracker;
		rollOver();
		return switch (t.scope) {
			case 1 -> t.today;
			case 2 -> t.allTime;
			default -> session;
		};
	}

	public static HudElement hud() {
		return new HudElement("Profit", () -> SkyMoleConfig.get().tracker.hud,
				() -> "Profit · " + SkyMoleConfig.Tracker.SCOPES[SkyMoleConfig.get().tracker.scope],
				ProfitTracker::rows, ProfitTracker::previewRows);
	}

	private static List<HudPanel.Row> rows() {
		if (!SkyBlockState.onMiningIsland()) return null;
		return rowsFor(current());
	}

	private static List<HudPanel.Row> previewRows() {
		Totals example = new Totals();
		example.mithril = 182_400;
		example.gemstone = 24_900;
		example.coins = 4_120_000;
		example.millis = 95 * 60_000L;
		return rowsFor(example);
	}

	private static List<HudPanel.Row> rowsFor(Totals t) {
		SkyMoleConfig.Tracker config = SkyMoleConfig.get().tracker;
		List<HudPanel.Row> rows = new ArrayList<>();
		double hours = t.millis / 3_600_000.0;
		if (config.powder) {
			if (t.mithril > 0) rows.add(row("Mithril", t.mithril, hours, config.perHour, Branding.MITHRIL));
			if (t.gemstone > 0) rows.add(row("Gemstone", t.gemstone, hours, config.perHour, Branding.SNOUT_PINK));
			if (t.glacite > 0) rows.add(row("Glacite", t.glacite, hours, config.perHour, 0xFF9FD8FF));
		}
		if (config.coins && t.coins > 0) {
			String value = PowderTracker.compact(t.coins);
			if (config.perHour && hours > 0.01) value += "  " + PowderTracker.compact(t.coins / hours) + "/h";
			rows.add(new HudPanel.Row("Coins", value, Branding.LAMP_GOLD, -1));
		}
		if (rows.isEmpty()) rows.add(HudPanel.Row.text("Nothing yet"));
		else rows.add(new HudPanel.Row("Time", PowderTracker.formatTime(t.millis), Branding.TEXT_DIM, -1));
		return rows;
	}

	private static HudPanel.Row row(String name, long amount, double hours, boolean perHour, int color) {
		String value = String.format(Locale.ROOT, "%,d", amount);
		if (perHour && hours > 0.01) value += "  " + PowderTracker.compact(amount / hours) + "/h";
		return new HudPanel.Row(name, value, color, -1);
	}
}
