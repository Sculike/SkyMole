package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.Prices;
import dev.sculike.skymole.ui.Cutscenes;

/**
 * Glacite Mineshaft corpses (Lapis, Umber, Tungsten, Vanguard): reads the loot from chat,
 * prices it, then plays the case-opening roulette and/or the big-drop celebration.
 */
public final class CorpseLoot {
	public record Reward(String item, long amount, double value) {}

	private static final Pattern HEADER = Pattern.compile("^(\\w+) CORPSE LOOT!");
	private static final Pattern AMOUNT_AFTER = Pattern.compile("^(.+?)\\s+x([\\d,]+)$");
	private static final Pattern AMOUNT_BEFORE = Pattern.compile("^\\+?([\\d,]+)\\s+(.+)$");
	/** Reward lines arrive right after the header; stop collecting after this. */
	private static final long COLLECT_MS = 1200;

	private static String corpse;
	private static long startedAt;
	private static final List<Reward> rewards = new ArrayList<>();

	private CorpseLoot() {}

	public static void register() {
		Chat.listen(text -> {
			Matcher header = HEADER.matcher(text);
			if (header.find()) {
				corpse = Events.pretty(header.group(1));
				startedAt = System.currentTimeMillis();
				rewards.clear();
				return;
			}
			if (corpse == null) return;
			Reward r = parse(text);
			if (r != null) rewards.add(r);
		});
	}

	/** One reward line, e.g. "Glacite Powder x1,050", "+1,050 Glacite Powder" or "Shattered Locket". */
	static Reward parse(String line) {
		String t = line.trim();
		String upper = t.toUpperCase(Locale.ROOT);
		if (t.isEmpty() || upper.equals("REWARDS") || upper.contains("CORPSE LOOT") || t.startsWith("▬")) return null;
		if (t.contains(": ")) return null; // someone's chat message, not loot
		String item;
		long amount = 1;
		Matcher after = AMOUNT_AFTER.matcher(t);
		Matcher before = AMOUNT_BEFORE.matcher(t);
		if (after.matches()) {
			item = after.group(1).trim();
			amount = Long.parseLong(after.group(2).replace(",", ""));
		} else if (before.matches()) {
			item = before.group(2).trim();
			amount = Long.parseLong(before.group(1).replace(",", ""));
		} else {
			item = t;
		}
		if (item.length() > 40 || !item.matches(".*[A-Za-z].*")) return null;
		double each = Prices.of(item);
		return new Reward(item, amount, each < 0 ? -1 : each * amount);
	}

	/** Called after every state update: when the loot lines are in, play the scene. */
	public static void tick() {
		if (corpse == null || System.currentTimeMillis() - startedAt < COLLECT_MS) return;
		String type = corpse;
		// Price them again now: a price looked up when the line arrived may have come in since.
		List<Reward> loot = new ArrayList<>();
		for (Reward r : rewards) {
			double each = Prices.of(r.item());
			loot.add(new Reward(r.item(), r.amount(), each < 0 ? -1 : each * r.amount()));
		}
		corpse = null;
		rewards.clear();
		if (!loot.isEmpty()) play(type, List.copyOf(loot));
	}

	/** Plays the scenes for a corpse's loot (also used by the test buttons). */
	public static void play(String corpseType, List<Reward> loot) {
		SkyMoleConfig.Fun fun = SkyMoleConfig.get().fun;
		Reward best = loot.get(0);
		for (Reward r : loot) {
			if (r.value() > best.value()) best = r;
			ProfitTracker.addCoins(r.value());
		}

		boolean big = fun.bigDrop && best.value() >= fun.bigDropMillions * 1_000_000.0;
		if (fun.corpseRoulette) Cutscenes.roulette(corpseType, loot, best, fun.rouletteSpeed == 1);
		if (big) Cutscenes.bigDrop(best.item(), best.value());
	}
}
