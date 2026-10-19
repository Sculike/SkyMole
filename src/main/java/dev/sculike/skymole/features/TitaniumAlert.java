package dev.sculike.skymole.features;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.MiningIsland;
import dev.sculike.skymole.hypixel.SkyBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

/**
 * "Titanium!" when Titanium appears near you (it spawns while you mine Mithril). Watches the
 * blocks around you for new Titanium (polished diorite), so it doesn't depend on chat text.
 * The new Titanium is outlined for a while; the outline hides behind blocks like a block outline.
 */
public final class TitaniumAlert {
	private static final int RADIUS = 8;
	private static final int SCAN_TICKS = 5;
	private static final long OUTLINE_MS = 30_000;
	private static final int COLOR = 0xFFE8E8F0;

	private static final Set<BlockPos> known = new HashSet<>();
	private static final Map<BlockPos, Long> fresh = new HashMap<>();
	private static BlockPos lastCenter;
	private static int ticks;

	private TitaniumAlert() {}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		SkyMoleConfig.Tools config = SkyMoleConfig.get().tools;
		if (!config.titaniumAlert || mc.level == null || SkyBlockState.island() != MiningIsland.DWARVEN_MINES) {
			known.clear();
			fresh.clear();
			lastCenter = null;
			return;
		}
		if (++ticks >= SCAN_TICKS) {
			ticks = 0;
			scan(mc);
		}
		draw(mc);
	}

	private static void scan(Minecraft mc) {
		BlockPos center = mc.player.blockPosition();
		BlockPos previous = lastCenter;
		// After a teleport (or the first scan) everything around you is "old": learn it silently.
		boolean silent = previous == null || previous.distManhattan(center) > 24;
		lastCenter = center;

		Set<BlockPos> now = new HashSet<>();
		boolean spawned = false;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
			if (!"polished_diorite".equals(BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).getPath())) continue;
			BlockPos p = pos.immutable();
			now.add(p);
			// Only blocks inside the area checked last time can be "new": walking into a new
			// area shouldn't make old (decoration) diorite look like fresh Titanium.
			boolean wasChecked = previous != null && chebyshev(p, previous) <= RADIUS;
			if (!silent && wasChecked && !known.contains(p)) {
				fresh.put(p, System.currentTimeMillis());
				spawned = true;
			}
		}
		if (spawned) Alerts.show("Titanium!", "Right next to you");
		// Forget Titanium that was mined; keep remembering what's still out of the scan area.
		known.removeIf(p -> p.closerThan(center, RADIUS) && !now.contains(p));
		known.addAll(now);
		fresh.keySet().removeIf(p -> !now.contains(p) && p.closerThan(center, RADIUS));
	}

	private static int chebyshev(BlockPos a, BlockPos b) {
		return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
	}

	private static void draw(Minecraft mc) {
		if (fresh.isEmpty() || !SkyMoleConfig.get().tools.titaniumOutline) return;
		long now = System.currentTimeMillis();
		try (var ignored = mc.collectPerTickGizmos()) {
			Iterator<Map.Entry<BlockPos, Long>> it = fresh.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<BlockPos, Long> e = it.next();
				if (now - e.getValue() > OUTLINE_MS) {
					it.remove();
					continue;
				}
				Gizmos.cuboid(e.getKey(), 0.01f, GizmoStyle.stroke(COLOR, 5f));
			}
		}
	}
}
