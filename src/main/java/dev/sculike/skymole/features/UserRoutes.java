package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.SkyBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

/**
 * Routes you record yourself by walking them.
 *
 * <p>The built-in routes only cover the Dwarven Mines, and they only know the spots SkyMole was
 * told about. This lets you walk your own line once and have it drawn back to you afterwards:
 * your Crystal Hollows loop, the way you like to reach a commission, the path round a mineshaft.
 *
 * <p>Points are dropped as you walk rather than typed in, because a route worth saving is one you
 * already know how to walk. A route remembers the island it was recorded on and is only drawn
 * there, so your Hollows loop does not appear in the Dwarven Mines.
 */
public final class UserRoutes {
	/** Drop a point once you are this far from the last one. Close enough to follow corners. */
	private static final double STEP = 4.0;
	/** A sanity limit, so a forgotten recording cannot grow without end. */
	private static final int MAX_POINTS = 400;

	private static String recording;
	private static String recordingIsland;
	private static final List<int[]> points = new ArrayList<>();

	private UserRoutes() {}

	public static boolean isRecording() {
		return recording != null;
	}

	public static String recordingName() {
		return recording;
	}

	public static int pointCount() {
		return points.size();
	}

	/** Starts a new recording. Returns false if one is already running. */
	public static boolean start(String name) {
		if (recording != null) return false;
		recording = name;
		recordingIsland = SkyBlockState.area();
		points.clear();
		return true;
	}

	/** Drops a point where you stand. Returns the number of points so far, or -1 if not recording. */
	public static int addPoint() {
		Minecraft mc = Minecraft.getInstance();
		if (recording == null || mc.player == null) return -1;
		if (points.size() >= MAX_POINTS) return points.size();
		BlockPos at = mc.player.blockPosition();
		points.add(new int[] {at.getX(), at.getY(), at.getZ()});
		return points.size();
	}

	/** Saves the recording. Returns how many points were kept, or -1 if nothing was running. */
	public static int stop() {
		if (recording == null) return -1;
		if (points.size() < 2) {
			cancel();
			return 0;
		}
		SkyMoleConfig.Routes routes = SkyMoleConfig.get().routes;
		routes.points.put(recording, new ArrayList<>(points));
		routes.island.put(recording, recordingIsland == null ? "" : recordingIsland);
		routes.showing = recording;
		SkyMoleConfig.save();
		int saved = points.size();
		cancel();
		return saved;
	}

	public static void cancel() {
		recording = null;
		recordingIsland = null;
		points.clear();
	}

	public static boolean show(String name) {
		SkyMoleConfig.Routes routes = SkyMoleConfig.get().routes;
		if (!routes.points.containsKey(name)) return false;
		routes.showing = name;
		SkyMoleConfig.save();
		return true;
	}

	public static void hide() {
		SkyMoleConfig.get().routes.showing = "";
		SkyMoleConfig.save();
	}

	public static boolean delete(String name) {
		SkyMoleConfig.Routes routes = SkyMoleConfig.get().routes;
		if (routes.points.remove(name) == null) return false;
		routes.island.remove(name);
		if (name.equals(routes.showing)) routes.showing = "";
		SkyMoleConfig.save();
		return true;
	}

	public static Set<String> names() {
		return SkyMoleConfig.get().routes.points.keySet();
	}

	/** Which island a route was recorded on, or "" when it was not recorded on one. */
	public static String islandOf(String name) {
		return SkyMoleConfig.get().routes.island.getOrDefault(name, "");
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		if (recording != null) record(mc);
		draw(mc);
	}

	/** Drops a point every {@link #STEP} blocks, so walking the route is all it takes. */
	private static void record(Minecraft mc) {
		if (points.size() >= MAX_POINTS) return;
		BlockPos at = mc.player.blockPosition();
		if (points.isEmpty()) {
			points.add(new int[] {at.getX(), at.getY(), at.getZ()});
			return;
		}
		int[] last = points.get(points.size() - 1);
		double dx = at.getX() - last[0], dy = at.getY() - last[1], dz = at.getZ() - last[2];
		if (Math.sqrt(dx * dx + dy * dy + dz * dz) >= STEP) {
			points.add(new int[] {at.getX(), at.getY(), at.getZ()});
		}
	}

	private static void draw(Minecraft mc) {
		SkyMoleConfig.Routes config = SkyMoleConfig.get().routes;
		if (!config.show) return;

		// While recording, you see the line you are laying down.
		List<int[]> line = recording != null ? points : config.points.get(config.showing);
		if (line == null || line.size() < 2) return;
		if (recording == null && !onItsIsland(config.showing)) return;

		int colour = recording != null ? Branding.SNOUT_PINK : Branding.LAMP_GOLD;
		try (var ignored = mc.collectPerTickGizmos()) {
			Vec3 prev = centre(line.get(0));
			for (int i = 1; i < line.size(); i++) {
				Vec3 next = centre(line.get(i));
				// Always on top: this is a path you recorded, not a hint about what is in the world.
				Gizmos.line(prev, next, colour, 3f).setAlwaysOnTop();
				prev = next;
			}
			Vec3 end = centre(line.get(line.size() - 1));
			String label = recording != null
					? "Recording " + recording + "  " + points.size() + " points"
					: config.showing;
			Gizmos.billboardText(label, end.add(0, 1.4, 0),
					TextGizmo.Style.forColorAndCentered(colour).withScale(0.5f));
			Gizmos.cuboid(BlockPos.containing(end), GizmoStyle.stroke(colour, 3f)).setAlwaysOnTop();
		}
	}

	/** A route is only drawn on the island it was walked on. */
	private static boolean onItsIsland(String name) {
		String island = islandOf(name);
		return island.isEmpty() || island.equalsIgnoreCase(SkyBlockState.area());
	}

	private static Vec3 centre(int[] p) {
		return new Vec3(p[0] + 0.5, p[1] + 0.2, p[2] + 0.5);
	}

	/** For /skymole debug. */
	public static String describe() {
		SkyMoleConfig.Routes config = SkyMoleConfig.get().routes;
		String saved = config.points.isEmpty() ? "none"
				: String.join(", ", config.points.keySet());
		return "recording=" + (recording == null ? "no" : recording + " (" + points.size() + ")")
				+ " showing=" + (config.showing.isEmpty() ? "none" : config.showing)
				+ " saved=" + saved;
	}

	/** Route names for command completion, lower case for matching. */
	public static List<String> suggestions(String typed) {
		String lower = typed.toLowerCase(Locale.ROOT);
		List<String> out = new ArrayList<>();
		for (String name : names()) {
			if (name.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(name);
		}
		return out;
	}
}
