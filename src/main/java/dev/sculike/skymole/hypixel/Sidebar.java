package dev.sculike.skymole.hypixel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Reads the sidebar scoreboard (right side of the screen) as plain text. */
public final class Sidebar {
	private Sidebar() {}

	/** The sidebar title, like "SKYBLOCK", or "" if there is none. */
	public static String title() {
		Objective objective = objective();
		return objective == null ? "" : TabList.clean(objective.getDisplayName().getString());
	}

	/** Sidebar lines from top to bottom. */
	public static List<String> lines() {
		List<String> out = new ArrayList<>();
		Objective objective = objective();
		if (objective == null) return out;

		Scoreboard scoreboard = Minecraft.getInstance().level.getScoreboard();
		List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
		entries.removeIf(PlayerScoreEntry::isHidden);
		entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());
		for (PlayerScoreEntry entry : entries) {
			// Hypixel puts the visible text in the team prefix/suffix around the entry name.
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			Component line = PlayerTeam.formatNameForTeam(team, entry.ownerName());
			out.add(TabList.clean(line.getString()));
		}
		return out;
	}

	private static Objective objective() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return null;
		return level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
	}
}
