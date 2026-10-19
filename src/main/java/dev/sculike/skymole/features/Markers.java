package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hud.HudElement;
import dev.sculike.skymole.hud.HudPanel;
import dev.sculike.skymole.hypixel.CommissionTargets;
import dev.sculike.skymole.hypixel.MiningIsland;
import dev.sculike.skymole.hypixel.RouteGraph;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.Spots;
import dev.sculike.skymole.hypixel.Spots.Spot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

/**
 * Commission waypoints. Every active commission gets a waypoint automatically (box, beam, name
 * and distance). Right-clicking one selects it: it's drawn brighter, gets a line along the fastest
 * walking route and an arrow on the HUD. The selection clears when you arrive or leave the island.
 */
public final class Markers {
	/** @param commission the commission this waypoint is for, or null (Emissary, /skymole marker) */
	public record Marker(String label, String note, BlockPos pos, MiningIsland island, String commission) {}

	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
	private static final int ARRIVE_DISTANCE = 6;
	private static final int ROUTE_TICKS = 20;
	private static final int AUTO_COLOR = 0xFFE0B040;

	/** The selected waypoint (right-clicked), with a route line. */
	private static Marker current;
	/** Automatic waypoints for your active commissions. */
	private static List<Marker> auto = List.of();
	private static List<Vec3> route = List.of();
	private static int routeTicks;
	private static RouteGraph dwarvenRoutes;
	private static boolean routesLoaded;

	private Markers() {}

	public static Marker current() {
		return current;
	}

	public static void clear() {
		current = null;
		route = List.of();
	}

	/** Called after every state update: one automatic waypoint per active commission. */
	public static void updateAuto() {
		MiningIsland island = SkyBlockState.island();
		SkyMoleConfig.Markers config = SkyMoleConfig.get().markers;
		if (island == null || !config.enabled || !config.autoCommissions) {
			auto = List.of();
			return;
		}
		String eventArea = Events.activeArea();
		List<Marker> out = new ArrayList<>();
		java.util.Set<String> active = new java.util.HashSet<>();
		for (SkyBlockState.Commission c : SkyBlockState.commissions()) {
			if (c.done()) continue;
			active.add(key(c.name()));
			if (isReached(c.name())) continue; // checkpoint reached: hidden until the commission is done
			CommissionTargets.Target target = CommissionTargets.forCommission(c.name(), island, false, eventArea);
			Spot spot = nearest(target.spots(), target.tunnels(), island);
			if (spot != null) out.add(new Marker(c.name(), target.note(), spot.pos(), island, c.name()));
		}
		auto = List.copyOf(out);

		// Forget checkpoints of commissions that are finished (claimed or replaced by a new one).
		if (island.hasCommissions() && !SkyBlockState.commissions().isEmpty()
				&& config.reached.removeIf(r -> !active.contains(r))) {
			SkyMoleConfig.save();
		}
	}

	private static String key(String commission) {
		return commission.toLowerCase(Locale.ROOT);
	}

	/** True if you already reached this commission's waypoint (a saved checkpoint). */
	public static boolean isReached(String commission) {
		return SkyMoleConfig.get().markers.reached.contains(key(commission));
	}

	private static void markReached(Marker m) {
		if (m.commission() == null || !SkyMoleConfig.get().markers.reached.add(key(m.commission()))) return;
		SkyMoleConfig.save();
		Alerts.chat("Reached " + m.label() + ". Checkpoint saved: this waypoint stays hidden until the commission is done "
				+ "(right-click the commission to show it again).");
	}

	private static RouteGraph routesFor(MiningIsland island) {
		if (island != MiningIsland.DWARVEN_MINES) return null; // only the Dwarven Mines network for now
		if (!routesLoaded) {
			routesLoaded = true;
			dwarvenRoutes = RouteGraph.load("/assets/skymole/routes/dwarven_mines.json");
		}
		return dwarvenRoutes;
	}

	private static void updateRoute(Minecraft mc) {
		RouteGraph graph = SkyMoleConfig.get().markers.routeLine ? routesFor(current.island()) : null;
		if (graph == null) {
			route = List.of();
			return;
		}
		BlockPos t = current.pos();
		List<double[]> path = graph.route(
				new double[] {mc.player.getX(), mc.player.getY(), mc.player.getZ()},
				new double[] {t.getX() + 0.5, t.getY(), t.getZ() + 0.5});
		List<Vec3> points = new ArrayList<>();
		for (double[] p : path) points.add(new Vec3(p[0], p[1] + 0.2, p[2]));
		route = points;
	}

	/** Right-click on a commission: mark where to do it, or the nearest Emissary when it's done. */
	public static void forCommission(String name, boolean done) {
		MiningIsland island = SkyBlockState.island();
		if (island == null) return;
		String eventArea = Events.activeArea();
		CommissionTargets.Target target = CommissionTargets.forCommission(name, island, done, eventArea);

		Spot spot = target.emissary() ? nearestEmissary(island) : nearest(target.spots(), target.tunnels(), island);
		if (spot == null) {
			Alerts.chat(name + ": " + (target.note().isEmpty() ? "no known spot" : target.note()) + ".");
			return;
		}
		if (current != null && current.pos().equals(spot.pos())) {
			clear();
			Alerts.chat("Route removed.");
			return;
		}
		// Asking for it again brings back a reached (hidden) commission waypoint.
		if (!done && SkyMoleConfig.get().markers.reached.remove(key(name))) SkyMoleConfig.save();
		select(new Marker(spot.name(), target.note(), spot.pos(), island, done ? null : name));
		Alerts.chat("Route to " + spot.name() + " (" + distance(spot.pos()) + "m)" + (target.note().isEmpty() ? "" : ". " + target.note()));
	}

	private static void select(Marker marker) {
		current = marker;
		route = List.of();
		routeTicks = ROUTE_TICKS; // work out the route right away
	}

	/** Development only: put a marker anywhere (used by the showcase test world). */
	public static void devMark(String label, BlockPos pos) {
		select(new Marker(label, "Test marker", pos, SkyBlockState.island(), null));
	}

	/** /skymole marker <name>: mark the nearest spot with that name. */
	public static boolean markByName(String name) {
		MiningIsland island = SkyBlockState.island();
		if (island == null) return false;
		Spot spot = nearest(List.of(name), null, island);
		if (spot == null) return false;
		select(new Marker(spot.name(), "", spot.pos(), island, null));
		return true;
	}

	/** Custom spots are saved as "ISLAND:Name". */
	private static String customKey(MiningIsland island, String name) {
		return island.name() + ":" + name;
	}

	/** Your own spots (from /skymole marker set) plus the built-in ones, for one island. */
	private static List<Spot> spotsOn(MiningIsland island) {
		List<Spot> out = new ArrayList<>();
		String prefix = island.name() + ":";
		for (Map.Entry<String, int[]> e : SkyMoleConfig.get().markers.custom.entrySet()) {
			if (!e.getKey().startsWith(prefix)) continue;
			int[] p = e.getValue();
			out.add(new Spot(island, e.getKey().substring(prefix.length()), false, null, new BlockPos(p[0], p[1], p[2])));
		}
		for (Spot s : Spots.all()) {
			if (s.island() != island) continue;
			// A custom spot with the same name replaces all built-in spots with that name.
			boolean replaced = out.stream().anyMatch(c -> c.name().equalsIgnoreCase(s.name()));
			if (!replaced) out.add(s);
		}
		return out;
	}

	/**
	 * The closest spot (by walking cost) with any of these names.
	 * @param tunnels true = only Glacite Tunnels spots, false = only the Mines above, null = either
	 */
	private static Spot nearest(List<String> names, Boolean tunnels, MiningIsland island) {
		Spot best = null;
		for (Spot s : spotsOn(island)) {
			if (names.stream().noneMatch(n -> n.equalsIgnoreCase(s.name()))) continue;
			// Your own spots (tunnels == null) always count; built-in ones must be on the right level.
			if (tunnels != null && s.tunnels() != null && !s.tunnels().equals(tunnels)) continue;
			if (best == null || walkCost(s.pos()) < walkCost(best.pos())) best = s;
		}
		return best;
	}

	private static Spot nearestEmissary(MiningIsland island) {
		Spot best = null;
		for (Spot s : spotsOn(island)) {
			if (!s.emissary()) continue;
			if (best == null || walkCost(s.pos()) < walkCost(best.pos())) best = s;
		}
		return best;
	}

	/**
	 * Roughly how far it is to walk: going up or down through the mines takes much longer
	 * than the straight-line distance, so height differences count three times.
	 */
	private static double walkCost(BlockPos pos) {
		var player = Minecraft.getInstance().player;
		if (player == null) return 0;
		BlockPos p = player.blockPosition();
		double dx = pos.getX() - p.getX();
		double dz = pos.getZ() - p.getZ();
		return Math.sqrt(dx * dx + dz * dz) + 3 * Math.abs(pos.getY() - p.getY());
	}

	private static int distance(BlockPos pos) {
		var player = Minecraft.getInstance().player;
		if (player == null) return 0;
		return (int) Math.round(Math.sqrt(player.blockPosition().distSqr(pos)));
	}

	/** Called every client tick: arrival check, route updates and drawing the waypoints. */
	public static void tick(Minecraft mc) {
		if (mc.player == null) return;
		if (current != null && SkyBlockState.island() != current.island()) clear();
		if (current != null && distance(current.pos()) <= ARRIVE_DISTANCE) {
			if (current.commission() != null) markReached(current);
			else Alerts.chat("Arrived at " + current.label() + ".");
			clear();
		}
		SkyMoleConfig.Markers config = SkyMoleConfig.get().markers;
		if (!config.enabled) return;
		// Reaching a commission's waypoint saves it as a checkpoint (hidden from now on).
		for (Marker m : auto) {
			if (distance(m.pos()) <= ARRIVE_DISTANCE) markReached(m);
		}
		if (current == null && auto.isEmpty()) return;

		if (current != null && ++routeTicks >= ROUTE_TICKS) {
			routeTicks = 0;
			updateRoute(mc);
		}

		try (var ignored = mc.collectPerTickGizmos()) {
			for (Marker m : auto) {
				if (current != null && m.pos().equals(current.pos())) continue; // drawn as the selected one
				if (m.commission() != null && isReached(m.commission())) continue; // checkpoint reached
				draw(m, distance(m.pos()), AUTO_COLOR, 0.45f, config.beam ? 2f : 0f);
			}
			if (current != null) {
				draw(current, distance(current.pos()), Branding.LAMP_GOLD, 0.6f, config.beam ? 4f : 0f);
				drawRoute(mc);
			}
		}
	}

	/** A waypoint: box on the spot, optional beam, name and distance above it. Visible through walls. */
	private static void draw(Marker m, int dist, int color, float textScale, float beamWidth) {
		BlockPos p = m.pos();
		Vec3 base = new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
		Gizmos.cuboid(p, GizmoStyle.strokeAndFill(color, 3f, (0x40 << 24) | (color & 0xFFFFFF))).setAlwaysOnTop();
		if (beamWidth > 0) Gizmos.line(base, base.add(0, 80, 0), color, beamWidth).setAlwaysOnTop();
		Gizmos.billboardText(m.label() + "  " + dist + "m", base.add(0, 2.5, 0),
				TextGizmo.Style.forColorAndCentered(color).withScale(textScale)).setAlwaysOnTop();
	}

	/** The route line: from you, along the walking network, to the selected waypoint. */
	private static void drawRoute(Minecraft mc) {
		if (route.isEmpty()) return;
		Vec3 from = mc.player.position().add(0, 0.2, 0);
		// Skip route points you've already walked past (the closest point onward).
		int startAt = 0;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < route.size(); i++) {
			double d = route.get(i).distanceToSqr(from);
			if (d < best) {
				best = d;
				startAt = i;
			}
		}
		Vec3 prev = from;
		for (int i = startAt; i < route.size(); i++) {
			Gizmos.line(prev, route.get(i), Branding.LAMP_GOLD, 3f).setAlwaysOnTop();
			prev = route.get(i);
		}
		BlockPos t = current.pos();
		Gizmos.line(prev, new Vec3(t.getX() + 0.5, t.getY() + 0.2, t.getZ() + 0.5), Branding.LAMP_GOLD, 3f).setAlwaysOnTop();
	}

	public static HudElement hud() {
		return new HudElement("Marker", () -> SkyMoleConfig.get().markers.hud, () -> "Marker",
				Markers::rows,
				() -> List.of(new HudPanel.Row("Royal Mines", "↗ 124m", Branding.LAMP_GOLD, -1)));
	}

	private static List<HudPanel.Row> rows() {
		if (current == null) return null;
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		BlockPos p = current.pos();
		double dx = p.getX() + 0.5 - player.getX();
		double dz = p.getZ() + 0.5 - player.getZ();
		// Minecraft yaw: 0 = south (+z), turning right increases it.
		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double diff = ((targetYaw - player.getYRot()) % 360 + 540) % 360 - 180;
		String arrow = ARROWS[Math.floorMod((int) Math.round(diff / 45), 8)];
		List<HudPanel.Row> rows = new ArrayList<>();
		rows.add(new HudPanel.Row(current.label(), arrow + " " + distance(p) + "m", Branding.LAMP_GOLD, -1));
		if (!current.note().isEmpty()) rows.add(HudPanel.Row.text(current.note()));
		return rows;
	}

	/** Saves where you stand as the spot for this name on this island, e.g. a better cave entrance. */
	public static BlockPos setCustom(String name) {
		var player = Minecraft.getInstance().player;
		MiningIsland island = SkyBlockState.island();
		if (player == null || island == null) return null;
		BlockPos pos = player.blockPosition();
		removeCustom(name);
		SkyMoleConfig.get().markers.custom.put(customKey(island, name), new int[] {pos.getX(), pos.getY(), pos.getZ()});
		SkyMoleConfig.save();
		return pos;
	}

	public static boolean removeCustom(String name) {
		MiningIsland island = SkyBlockState.island();
		if (island == null) return false;
		String key = customKey(island, name).toLowerCase(Locale.ROOT);
		boolean removed = SkyMoleConfig.get().markers.custom.keySet().removeIf(k -> k.toLowerCase(Locale.ROOT).equals(key));
		if (removed) SkyMoleConfig.save();
		return removed;
	}

	/** All spot names on your island, for command suggestions. */
	public static List<String> names() {
		MiningIsland island = SkyBlockState.island();
		if (island == null) return List.of();
		return spotsOn(island).stream().map(Spot::name).distinct().sorted().toList();
	}
}
