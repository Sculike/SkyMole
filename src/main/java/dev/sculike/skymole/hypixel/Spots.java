package dev.sculike.skymole.hypixel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;

import dev.sculike.skymole.SkyMole;
import net.minecraft.core.BlockPos;

/**
 * Known spots on the mining islands (areas, ore caves, Emissaries), from assets/skymole/markers.json.
 * Each area becomes one spot at its heart; ore spots stay separate so the nearest one can be picked.
 * Locations based on SkyHanni-REPO (MIT), see markers-LICENSE.txt.
 */
public final class Spots {
	/** @param tunnels true = in the Glacite Tunnels, false = the Dwarven Mines above, null = unknown (your own spots) */
	public record Spot(MiningIsland island, String name, boolean emissary, Boolean tunnels, BlockPos pos) {}

	private record Raw(String island, String name, boolean emissary, boolean tunnels, boolean area, int[] pos) {}

	private static List<Spot> spots = List.of();

	/**
	 * SkyMole's own commission spots in the Dwarven Mines, measured in game (Oct 2026):
	 * where you actually want to stand for each area's commissions.
	 */
	private static final Map<String, BlockPos> OWN_DWARVEN = Map.of(
			"Lava Springs", new BlockPos(50, 198, -15),
			"Cliffside Veins", new BlockPos(36, 128, 33),
			"Royal Mines", new BlockPos(113, 154, 40),
			"Great Ice Wall", new BlockPos(0, 128, 150),
			"Goblin Burrows", new BlockPos(-73, 138, 150),
			"Rampart's Quarry", new BlockPos(-101, 148, 0),
			"Upper Mines", new BlockPos(-109, 167, -70));

	private Spots() {}

	public static void load() {
		try (InputStream in = Spots.class.getResourceAsStream("/assets/skymole/markers.json")) {
			if (in == null) return;
			Raw[] raw = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Raw[].class);
			List<Spot> out = new ArrayList<>();
			// Area points (many per area, spread over it) are grouped and replaced by one heart.
			Map<String, List<Spot>> areas = new LinkedHashMap<>();
			for (Raw r : raw) {
				MiningIsland island;
				try {
					island = MiningIsland.valueOf(r.island());
				} catch (IllegalArgumentException e) {
					continue;
				}
				Spot s = new Spot(island, r.name(), r.emissary(), r.tunnels(), new BlockPos(r.pos()[0], r.pos()[1], r.pos()[2]));
				if (r.area()) {
					String key = island + "|" + r.tunnels() + "|" + r.name().toLowerCase(Locale.ROOT);
					areas.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
				} else {
					out.add(s);
				}
			}
			for (List<Spot> points : areas.values()) {
				Spot heart = heartOf(points);
				// SkyMole's own measured spot wins over the computed heart.
				BlockPos own = heart.island() == MiningIsland.DWARVEN_MINES && Boolean.FALSE.equals(heart.tunnels())
						? OWN_DWARVEN.get(heart.name()) : null;
				out.add(own != null ? new Spot(heart.island(), heart.name(), false, false, own) : heart);
			}
			spots = List.copyOf(out);
		} catch (Exception e) {
			SkyMole.LOGGER.error("Couldn't load marker spots", e);
		}
	}

	/**
	 * The heart of an area: the point closest to the middle of all its points. Using a real point
	 * (not the exact middle) keeps it somewhere you can actually walk, even for odd-shaped areas.
	 */
	static Spot heartOf(List<Spot> points) {
		double x = 0, y = 0, z = 0;
		for (Spot s : points) {
			x += s.pos().getX();
			y += s.pos().getY();
			z += s.pos().getZ();
		}
		int n = points.size();
		double cx = x / n, cy = y / n, cz = z / n;
		Spot best = points.get(0);
		double bestDist = Double.MAX_VALUE;
		for (Spot s : points) {
			double dx = s.pos().getX() - cx, dy = s.pos().getY() - cy, dz = s.pos().getZ() - cz;
			double d = dx * dx + dy * dy + dz * dz;
			if (d < bestDist) {
				bestDist = d;
				best = s;
			}
		}
		return best;
	}

	public static List<Spot> all() {
		return spots;
	}
}
