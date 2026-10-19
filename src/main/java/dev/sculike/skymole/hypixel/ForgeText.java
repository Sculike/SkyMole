package dev.sculike.skymole.hypixel;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads forge slot text from Hypixel, like "1) Refined Mithril: 1h 23m" or "Ready!". */
public final class ForgeText {
	/** One slot: its number (0 if unknown), the item, and millis left (0 = ready, -1 = unknown). */
	public record Slot(int number, String item, long millisLeft) {}

	private static final Pattern TAB_LINE = Pattern.compile("^(\\d+)\\)\\s*(.+?):\\s*(.+)$");
	private static final Pattern PART = Pattern.compile("(\\d+)\\s*([dhms])\\b", Pattern.CASE_INSENSITIVE);

	private ForgeText() {}

	/** "1h 23m 4s" -> millis, "Ready!" -> 0, anything else -> -1. */
	public static long millisLeft(String text) {
		String t = text.trim().toLowerCase(Locale.ROOT);
		if (t.contains("ready") || t.contains("complete")) return 0;
		Matcher m = PART.matcher(t);
		long total = 0;
		boolean found = false;
		while (m.find()) {
			found = true;
			long n = Long.parseLong(m.group(1));
			total += switch (m.group(2)) {
				case "d" -> n * 86_400_000L;
				case "h" -> n * 3_600_000L;
				case "m" -> n * 60_000L;
				default -> n * 1_000L;
			};
		}
		return found ? total : -1;
	}

	/** A line from the tab list's Forge widget, e.g. "1) Refined Mithril: 1h 23m", or null. */
	public static Slot tabLine(String line) {
		Matcher m = TAB_LINE.matcher(line.trim());
		if (!m.matches()) return null;
		long left = millisLeft(m.group(3));
		if (left < 0) return null; // e.g. "1) Empty" or a locked slot
		return new Slot(Integer.parseInt(m.group(1)), m.group(2).trim(), left);
	}
}
