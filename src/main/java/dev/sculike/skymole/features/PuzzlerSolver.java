package dev.sculike.skymole.features;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

/**
 * The Puzzler (Dwarven Mines) gives a row of arrows. Starting from a fixed block next to him
 * and following the arrows leads to the block you have to mine; SkyMole outlines it.
 */
public final class PuzzlerSolver {
	/** Where the arrows start counting from (the same method community solvers use). */
	private static final int START_X = 181, START_Z = 135, Y = 195;
	private static final long SHOW_MS = 120_000;

	private static BlockPos answer;
	private static long shownAt;

	private PuzzlerSolver() {}

	/** Follows the arrows: ▲ +z, ▼ -z, ◀ +x, ▶ -x. Returns {x, z}. */
	static int[] solve(String arrows) {
		int x = START_X, z = START_Z;
		for (char c : arrows.toCharArray()) {
			switch (c) {
				case '▲' -> z++; // ▲
				case '▼' -> z--; // ▼
				case '◀' -> x++; // ◀
				case '▶' -> x--; // ▶
				default -> { }
			}
		}
		return new int[] {x, z};
	}

	public static void register() {
		Chat.listen(text -> {
			if (!text.startsWith("[NPC] Puzzler:")) return;
			String said = text.substring("[NPC] Puzzler:".length());
			if (said.contains("Nice!") || said.contains("Come back")) {
				answer = null; // solved, or come back tomorrow
				return;
			}
			if (!SkyMoleConfig.get().solvers.puzzler || said.chars().noneMatch(c -> c >= 0x25B2 && c <= 0x25C0)) return;
			int[] xz = solve(said);
			answer = new BlockPos(xz[0], Y, xz[1]);
			shownAt = System.currentTimeMillis();
			Alerts.chat("Puzzler: mine the outlined block (" + xz[0] + ", " + Y + ", " + xz[1] + ").");
		});
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		if (answer == null) return;
		if (System.currentTimeMillis() - shownAt > SHOW_MS) {
			answer = null;
			return;
		}
		try (var ignored = mc.collectPerTickGizmos()) {
			Gizmos.cuboid(answer, 0.01f, GizmoStyle.strokeAndFill(Branding.GOOD, 4f, 0x507BD88F)).setAlwaysOnTop();
			Gizmos.billboardText("Mine this", Vec3.atCenterOf(answer).add(0, 1.2, 0),
					TextGizmo.Style.forColorAndCentered(Branding.GOOD).withScale(0.5f)).setAlwaysOnTop();
		}
	}
}
