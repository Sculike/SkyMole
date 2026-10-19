package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hud.HudElement;
import dev.sculike.skymole.hud.HudManager;
import dev.sculike.skymole.hud.HudPanel;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.CommissionGoals;
import dev.sculike.skymole.hypixel.MiningIsland;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.SkyBlockState.Commission;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/** Commission HUD, alerts for finished commissions, messages for new ones, and a claim reminder. */
public final class Commissions {
	// Last known state per commission name, to spot new and finished commissions.
	private static final Map<String, Boolean> lastDone = new HashMap<>();
	private static MiningIsland lastIsland;
	private static boolean allDoneReminded;

	private Commissions() {}

	private static final List<Commission> PREVIEW = List.of(
			new Commission("Royal Mines Titanium", 40, false),
			new Commission("2x Mithril Powder Collector", 72, false),
			new Commission("Goblin Raid Slayer", 100, true),
			new Commission("Glacite Walker Slayer", 10, false));

	public static HudElement hud() {
		return new HudElement("Commissions", () -> SkyMoleConfig.get().commissions.hud, () -> "Commissions",
				Commissions::rows,
				() -> rowsFor(PREVIEW, MiningIsland.DWARVEN_MINES, SkyMoleConfig.get().commissions));
	}

	/** Called after every state update. */
	public static void tick() {
		MiningIsland island = SkyBlockState.island();
		List<Commission> current = SkyBlockState.commissions();
		if (island != lastIsland) {
			// New island or lobby: remember what's there without alerting about it.
			lastIsland = island;
			lastDone.clear();
			for (Commission c : current) lastDone.put(c.name(), c.done());
			allDoneReminded = !current.isEmpty() && current.stream().allMatch(Commission::done);
			return;
		}
		// The tab list can briefly be empty while it loads; don't treat that as "all new".
		if (current.isEmpty()) return;

		for (Commission c : current) {
			Boolean wasDone = lastDone.get(c.name());
			if (wasDone == null && !lastDone.isEmpty()) newCommission(c, island);
			else if (c.done() && wasDone != null && !wasDone) alertDone(c.name());
		}
		lastDone.clear();
		for (Commission c : current) lastDone.put(c.name(), c.done());

		boolean allDone = current.stream().allMatch(Commission::done);
		if (allDone && !allDoneReminded && SkyMoleConfig.get().commissions.claimReminder) {
			Alerts.show("All commissions done!", "Go claim them");
		}
		allDoneReminded = allDone;
	}

	private static void newCommission(Commission c, MiningIsland island) {
		if (!SkyMoleConfig.get().commissions.newCommissionMessage) return;
		int goal = CommissionGoals.goal(c.name(), island);
		Alerts.chat("New commission: " + c.name() + (goal > 0 ? " (" + goal + ")" : ""));
	}

	/** Hypixel's chat line, e.g. "CLIFFSIDE VEINS MITHRIL Commission Complete! Visit the King...". */
	private static final java.util.regex.Pattern COMPLETE = java.util.regex.Pattern.compile("^(.+?) Commission Complete!");
	/** When each commission was last alerted (lower case name), so chat + tab list don't alert twice. */
	private static final Map<String, Long> lastAlert = new HashMap<>();

	public static void register() {
		// Chat is instant; the tab list can lag a second or two behind.
		Chat.listen(text -> {
			java.util.regex.Matcher m = COMPLETE.matcher(text);
			if (m.find()) alertDone(Events.pretty(m.group(1).trim()));
		});
	}

	public static void alertDone(String name) {
		long now = System.currentTimeMillis();
		Long last = lastAlert.put(name.toLowerCase(Locale.ROOT), now);
		if (last != null && now - last < 60_000) return;
		if (SkyMoleConfig.get().commissions.doneAlert) Alerts.show("Commission done!", name);
	}

	/** The commission on the HUD under the mouse (chat open), or null. */
	public static Commission at(double mouseX, double mouseY) {
		MiningIsland island = SkyBlockState.island();
		if (island == null || !island.hasCommissions()) return null;
		List<Commission> list = SkyBlockState.commissions();
		int row = HudManager.rowAt("Commissions", SkyMoleConfig.get().commissions.hud, mouseX, mouseY);
		return row >= 0 && row < list.size() ? list.get(row) : null;
	}

	private static List<HudPanel.Row> rows() {
		MiningIsland island = SkyBlockState.island();
		if (island == null || !island.hasCommissions()) return null;
		List<Commission> list = SkyBlockState.commissions();
		if (list.isEmpty()) {
			if (!SkyBlockState.commissionWidgetFound()) {
				return List.of(HudPanel.Row.text("Turn on the Commissions"), HudPanel.Row.text("widget in your tab list"));
			}
			return List.of(HudPanel.Row.text("No commissions"));
		}
		return rowsFor(list, island, SkyMoleConfig.get().commissions);
	}

	private static List<HudPanel.Row> rowsFor(List<Commission> list, MiningIsland island, SkyMoleConfig.Commissions config) {
		List<HudPanel.Row> rows = new ArrayList<>();
		for (Commission c : list) {
			float progress = config.showBar() ? c.percent() / 100f : -1;
			String value = "";
			int color = Branding.TEXT_DIM;
			if (c.done()) {
				value = config.display == 5 ? "" : "Done";
				color = Branding.GOOD;
			} else if (config.showPercent()) {
				value = formatPercent(c.percent());
			} else if (config.showCount()) {
				String count = CommissionGoals.count(c.name(), island, c.percent());
				value = count != null ? count : formatPercent(c.percent());
			}
			// ✔ = you already reached this commission's waypoint (checkpoint saved).
			String label = !c.done() && Markers.isReached(c.name()) ? c.name() + " ✔" : c.name();
			rows.add(new HudPanel.Row(label, value, color, progress));
		}
		// Hint under the list (after the commissions, so it doesn't shift which row is which).
		if (config.waypointHint && SkyMoleConfig.get().markers.enabled) {
			boolean chatOpen = Minecraft.getInstance().gui.screen() instanceof ChatScreen;
			rows.add(new HudPanel.Row(chatOpen ? "Right-click a commission for a route" : "Right-click (chat or menu) = route",
					"", Branding.MITHRIL, -1));
		}
		return rows;
	}

	private static String formatPercent(float percent) {
		return percent == Math.floor(percent) ? (int) percent + "%" : String.format(Locale.ROOT, "%.1f%%", percent);
	}
}
