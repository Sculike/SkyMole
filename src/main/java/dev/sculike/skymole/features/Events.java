package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import dev.sculike.skymole.hypixel.TabList;
import dev.sculike.skymole.mixin.BossHealthOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * Mining events, read from Hypixel's boss bar, e.g. "EVENT RAFFLE ACTIVE IN FAR RESERVE for 02:05".
 * Also alerts for Golden and Diamond Goblins from chat.
 */
public final class Events {
	public record Event(String name, String where, String timeLeft) {}

	/** Known events, as they appear (upper case) in the boss bar. */
	private static final String[] KNOWN = {
			"2X POWDER", "GOBLIN RAID", "RAFFLE", "BETTER TOGETHER", "GONE WITH THE WIND", "MITHRIL GOURMAND"};
	private static final Pattern TIME = Pattern.compile("(\\d+:\\d{2}(?::\\d{2})?)");
	private static final Pattern WHERE = Pattern.compile(" IN (.+?)(?: FOR | for |$)");

	private static Map<String, Event> active = Map.of();

	private Events() {}

	/** "Find the Powder Ghast near the Divan's Gateway!" */
	private static final Pattern POWDER_GHAST = Pattern.compile("^Find the Powder Ghast near the (.+?)!");

	public static void register() {
		Chat.listen(text -> {
			Matcher ghast = POWDER_GHAST.matcher(text);
			if (ghast.find() && SkyMoleConfig.get().events.powderGhastAlert) {
				String place = ghast.group(1).trim();
				Alerts.show("Powder Ghast!", "Near " + place);
				Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
						Branding.chat("Powder Ghast near " + place + " ")
								.append(Component.literal("[Mark it]").withColor(Branding.LAMP_GOLD & 0xFFFFFF)
										.withStyle(style -> style
												.withClickEvent(new ClickEvent.RunCommand("/skymole marker " + place))
												.withHoverEvent(new HoverEvent.ShowText(Component.literal("Put a SkyMole marker there"))))));
				return;
			}
			if (!SkyMoleConfig.get().events.goblinAlert || !SkyBlockState.onMiningIsland()) return;
			String lower = text.toLowerCase(Locale.ROOT);
			if (lower.contains("spawned") || lower.contains("appeared")) {
				if (lower.contains("golden goblin")) Alerts.show("Golden Goblin!", "Spawned nearby");
				else if (lower.contains("diamond goblin")) Alerts.show("Diamond Goblin!", "Spawned nearby");
			}
		});
	}

	public static boolean isActive(String name) {
		return active.containsKey(name);
	}

	/** Where the current event with a location is (e.g. "Far Reserve"), or "". */
	public static String activeArea() {
		for (Event e : active.values()) {
			if (!e.where().isEmpty()) return e.where();
		}
		return "";
	}

	/** Called after every state update. */
	public static void tick() {
		Map<String, Event> now = new LinkedHashMap<>();
		if (SkyBlockState.onMiningIsland()) {
			var overlay = (BossHealthOverlayAccessor) Minecraft.getInstance().gui.hud.getBossOverlay();
			for (LerpingBossEvent bar : overlay.skymole$events().values()) {
				Event e = parse(TabList.clean(bar.getName().getString()));
				if (e != null) now.put(e.name(), e);
			}
		}
		SkyMoleConfig.Events config = SkyMoleConfig.get().events;
		for (String name : now.keySet()) {
			if (!active.containsKey(name) && config.startAlert) Alerts.show(pretty(name) + " started!", now.get(name).where());
		}
		for (String name : active.keySet()) {
			if (!now.containsKey(name) && config.endAlert) Alerts.chat(pretty(name) + " ended.");
		}
		active = now;
	}

	static Event parse(String text) {
		String upper = text.toUpperCase(Locale.ROOT);
		if (!upper.contains("EVENT")) return null;
		for (String name : KNOWN) {
			if (!upper.contains(name)) continue;
			Matcher time = TIME.matcher(text);
			String timeLeft = "";
			while (time.find()) timeLeft = time.group(1); // the last time on the bar
			Matcher where = WHERE.matcher(upper);
			String place = where.find() ? pretty(where.group(1).trim()) : "";
			return new Event(name, place, timeLeft);
		}
		return null;
	}

	/** "GOBLIN RAID" -> "Goblin Raid", "2X POWDER" -> "2x Powder". */
	static String pretty(String upper) {
		StringBuilder out = new StringBuilder();
		for (String word : upper.toLowerCase(Locale.ROOT).split(" ")) {
			if (word.isEmpty()) continue;
			if (!out.isEmpty()) out.append(' ');
			out.append(word.matches("\\d+x") ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
		}
		return out.toString();
	}

	public static HudElement hud() {
		return new HudElement("Events", () -> SkyMoleConfig.get().events.hud, () -> "Events",
				Events::rows,
				() -> List.of(
						new HudPanel.Row("Raffle", "Far Reserve  2:05", Branding.LAMP_GOLD, -1),
						new HudPanel.Row("2x Powder", "14:32", Branding.MITHRIL, -1)));
	}

	private static List<HudPanel.Row> rows() {
		if (active.isEmpty()) return null;
		List<HudPanel.Row> rows = new ArrayList<>();
		for (Event e : active.values()) {
			String value = (e.where().isEmpty() ? "" : e.where() + "  ") + e.timeLeft();
			int color = e.name().equals("2X POWDER") ? Branding.MITHRIL : Branding.LAMP_GOLD;
			rows.add(new HudPanel.Row(pretty(e.name()), value.trim(), color, -1));
		}
		return rows;
	}
}
