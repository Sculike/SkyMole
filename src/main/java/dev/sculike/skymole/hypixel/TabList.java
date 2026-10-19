package dev.sculike.skymole.hypixel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

/** Reads the tab list as plain text lines, in the same order the game shows them. */
public final class TabList {
	// Same order as vanilla: by team name, then profile name. Hypixel names its
	// fake tab entries so this gives top-to-bottom, column-by-column order.
	private static final Comparator<PlayerInfo> ORDER = Comparator
			.comparing((PlayerInfo info) -> {
				PlayerTeam team = info.getTeam();
				return team != null ? team.getName() : "";
			})
			.thenComparing(info -> info.getProfile().name(), String::compareToIgnoreCase);

	private TabList() {}

	public static List<String> lines() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		List<String> out = new ArrayList<>();
		if (connection == null) return out;

		List<PlayerInfo> infos = new ArrayList<>(connection.getListedOnlinePlayers());
		infos.sort(ORDER);
		for (PlayerInfo info : infos) {
			Component name = info.getTabListDisplayName();
			if (name == null) continue;
			out.add(clean(name.getString()));
		}
		return out;
	}

	/**
	 * Strips color codes and odd spacing so lines can be matched as plain text. Hypixel also
	 * puts fake codes like "§q" in the middle of scoreboard words ("Upper Min§qes"), so every
	 * "§" plus the character after it is removed, not just real color codes.
	 */
	public static String clean(String raw) {
		if (raw == null) return "";
		String stripped = ChatFormatting.stripFormatting(raw.replaceAll("§.", ""));
		return stripped == null ? "" : stripped.replace(' ', ' ').stripTrailing();
	}
}
