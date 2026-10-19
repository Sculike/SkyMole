package dev.sculike.skymole.hypixel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything SkyMole knows about where you are and what you're doing,
 * read from the tab list and sidebar about twice a second.
 */
public final class SkyBlockState {
	public record Commission(String name, float percent, boolean done) {}

	private static final Pattern AREA = Pattern.compile("^(?:Area|Dungeon): (.+)$");
	private static final Pattern COMMISSION = Pattern.compile("^(.+?): (?:(\\d+(?:\\.\\d+)?)%|(DONE))$");
	private static final Pattern POWDER = Pattern.compile("^(Mithril|Gemstone|Glacite)(?: Powder)?: ([\\d,]+)$");
	private static final Pattern COLD = Pattern.compile("Cold: -?(\\d+)");

	private static boolean inSkyBlock;
	private static String area = "";
	private static String zone = "";
	private static int cold;
	private static MiningIsland island;
	private static List<Commission> commissions = List.of();
	private static boolean commissionWidgetFound;
	private static Map<String, Long> powders = Map.of();
	private static Map<String, List<String>> sections = Map.of();

	private SkyBlockState() {}

	public static void update() {
		String title = Sidebar.title().toUpperCase(Locale.ROOT);
		inSkyBlock = title.contains("SKYBLOCK");
		if (!inSkyBlock) {
			area = "";
			zone = "";
			cold = 0;
			island = null;
			commissions = List.of();
			commissionWidgetFound = false;
			powders = Map.of();
			sections = Map.of();
			return;
		}
		parseSidebar(Sidebar.lines());
		parseTabList(TabList.lines());
	}

	/** Zone names on the mining islands, for sidebars where the ⏣ symbol doesn't come through. */
	private static final Set<String> KNOWN_ZONES = Set.of(
			"dwarven village", "royal mines", "royal palace", "royal quarters", "palace bridge", "lava springs",
			"cliffside veins", "rampart's quarry", "upper mines", "far reserve", "goblin burrows", "great ice wall",
			"the mist", "divan's gateway", "hanging court", "aristocrat passage", "barracks of heroes",
			"gates to the mines", "miner's guild", "forge basin", "the forge", "grand library", "dwarven tavern",
			"abandoned quarry", "the lift", "glacite tunnels", "dwarven base camp", "great glacite lake",
			"fossil research center", "glacite mineshafts", "jungle", "jungle temple", "goblin holdout",
			"goblin queen's den", "magma fields", "khazad-dûm", "mithril deposits", "mines of divan",
			"precursor remnants", "lost precursor city", "crystal nucleus", "fairy grotto", "dragon's lair",
			"gold mine", "deep caverns");

	private static boolean isKnownZone(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		if (KNOWN_ZONES.contains(lower)) return true;
		for (Spots.Spot s : Spots.all()) {
			if (s.name().equalsIgnoreCase(name)) return true;
		}
		return false;
	}

	static void parseSidebar(List<String> lines) {
		String newZone = "";
		int newCold = 0;
		for (String raw : lines) {
			String line = raw.trim();
			if (line.startsWith("⏣")) {
				// Usual zone line: "⏣ Royal Mines"
				newZone = line.substring(1).trim();
			} else if (newZone.isEmpty()) {
				// Sometimes the symbol is missing: drop any leading symbols and check the name.
				String name = line.replaceFirst("^[^\\p{L}]+", "").trim();
				if (!name.isEmpty() && isKnownZone(name)) newZone = name;
			}
			Matcher m = COLD.matcher(line);
			if (m.find()) newCold = Integer.parseInt(m.group(1));
		}
		zone = newZone;
		cold = newCold;
	}

	static void parseTabList(List<String> lines) {
		String newArea = "";
		List<Commission> newCommissions = new ArrayList<>();
		Map<String, Long> newPowders = new LinkedHashMap<>();
		Map<String, List<String>> newSections = new LinkedHashMap<>();
		boolean foundCommissions = false;

		String section = "";
		for (String raw : lines) {
			String line = raw.trim();
			if (line.isEmpty()) {
				section = "";
				continue;
			}

			Matcher area = AREA.matcher(line);
			if (area.matches()) {
				newArea = area.group(1).trim();
				section = "";
				continue;
			}

			// Section headers ("Commissions:", "Powders:") end with a colon and aren't indented.
			// Their entries are indented with a leading space.
			boolean indented = raw.startsWith(" ");
			if (!indented && line.endsWith(":")) {
				section = line.substring(0, line.length() - 1);
				newSections.putIfAbsent(section, new ArrayList<>());
				if (section.equals("Commissions")) foundCommissions = true;
				continue;
			}
			if (!indented) {
				section = "";
				continue;
			}
			if (!section.isEmpty()) newSections.get(section).add(line);

			if (section.equals("Commissions")) {
				Matcher m = COMMISSION.matcher(line);
				if (m.matches()) {
					boolean done = m.group(3) != null;
					float percent = done ? 100f : Float.parseFloat(m.group(2));
					newCommissions.add(new Commission(m.group(1), percent, done));
				}
			} else if (section.equals("Powders")) {
				Matcher m = POWDER.matcher(line);
				if (m.matches()) newPowders.put(m.group(1), Long.parseLong(m.group(2).replace(",", "")));
			}
		}

		area = newArea;
		island = MiningIsland.fromArea(newArea);
		commissions = List.copyOf(newCommissions);
		commissionWidgetFound = foundCommissions;
		powders = Map.copyOf(newPowders);
		newSections.replaceAll((k, v) -> List.copyOf(v));
		sections = Map.copyOf(newSections);
	}

	public static boolean inSkyBlock() { return inSkyBlock; }
	public static String area() { return area; }
	/** The zone inside the island, like "Royal Mines" or "Jungle". */
	public static String zone() { return zone; }
	/** Glacite Tunnels cold, as a positive number (0 when not cold). */
	public static int cold() { return cold; }
	/** The mining island you're on, or null if you're not on one. */
	public static MiningIsland island() { return island; }
	public static boolean onMiningIsland() { return island != null; }
	public static List<Commission> commissions() { return commissions; }
	/** False when the Commissions widget is turned off in Hypixel's tab list settings. */
	public static boolean commissionWidgetFound() { return commissionWidgetFound; }
	public static Map<String, Long> powders() { return powders; }
	/** Lines of a tab list widget, e.g. section("Crystals"), or an empty list. */
	public static List<String> section(String name) { return sections.getOrDefault(name, List.of()); }
	public static Map<String, List<String>> sections() { return sections; }
}
