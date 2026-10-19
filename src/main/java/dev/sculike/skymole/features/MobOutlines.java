package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.MiningMobs;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.TabList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

/**
 * Outlines mining mobs (and the Forge NPCs while you're at the Forge) in their own color.
 * The outline is drawn like a block outline: hidden behind blocks, so it never shows mobs
 * through walls (that would be ESP).
 */
public final class MobOutlines {
	private record Target(Entity body, Entity nameTag, int color) {}

	private static final double RANGE = 32;
	private static final int RESCAN_TICKS = 5;

	private static List<Target> targets = List.of();
	private static int ticks;

	private MobOutlines() {}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		SkyMoleConfig.Area config = SkyMoleConfig.get().area;
		if (mc.player == null || mc.level == null || !SkyBlockState.onMiningIsland()
				|| (!config.mobOutlines && !config.forgeOutline)) {
			targets = List.of();
			return;
		}
		if (++ticks >= RESCAN_TICKS) {
			ticks = 0;
			targets = scan(mc.level, mc, config);
		}
		draw(mc);
	}

	private static List<Target> scan(ClientLevel level, Minecraft mc, SkyMoleConfig.Area config) {
		boolean atForge = SkyBlockState.zone().toLowerCase(Locale.ROOT).contains("forge");
		List<Target> out = new ArrayList<>();
		for (Entity e : level.entitiesForRendering()) {
			// Hypixel puts mob and NPC names on invisible armor stands floating above them.
			if (!(e instanceof ArmorStand) || !e.hasCustomName() || e.distanceTo(mc.player) > RANGE) continue;
			String tag = TabList.clean(e.getCustomName().getString());
			int color;
			if (config.mobOutlines && MiningMobs.match(tag) != null) {
				color = MiningMobs.match(tag).color();
			} else if (config.forgeOutline && atForge && MiningMobs.isForgeNpc(tag)) {
				color = Branding.LAMP_GOLD;
			} else {
				continue;
			}
			out.add(new Target(bodyUnder(level, e, mc), e, color));
		}
		return out;
	}

	/** The mob right under a name tag: the closest living thing just below it (null if none). */
	private static Entity bodyUnder(ClientLevel level, Entity tag, Minecraft mc) {
		Entity best = null;
		double bestDist = Double.MAX_VALUE;
		AABB below = new AABB(tag.getX() - 1.5, tag.getY() - 3.5, tag.getZ() - 1.5, tag.getX() + 1.5, tag.getY() + 0.5, tag.getZ() + 1.5);
		for (Entity e : level.entitiesForRendering()) {
			if (e == mc.player || e instanceof ArmorStand || !(e instanceof LivingEntity) || !e.isAlive()) continue;
			if (!below.contains(e.position())) continue;
			double d = e.position().distanceToSqr(tag.position());
			if (d < bestDist) {
				bestDist = d;
				best = e;
			}
		}
		return best;
	}

	private static void draw(Minecraft mc) {
		if (targets.isEmpty()) return;
		try (var ignored = mc.collectPerTickGizmos()) {
			for (Target t : targets) {
				AABB box;
				if (t.body() != null && t.body().isAlive()) {
					box = t.body().getBoundingBox().inflate(0.05);
				} else if (t.nameTag().isAlive()) {
					// No separate body found: a body-sized box under the name tag.
					double x = t.nameTag().getX(), y = t.nameTag().getY(), z = t.nameTag().getZ();
					box = new AABB(x - 0.4, y - 2.0, z - 0.4, x + 0.4, y, z + 0.4);
				} else {
					continue;
				}
				// No setAlwaysOnTop: hidden behind blocks, so no seeing mobs through walls.
				Gizmos.cuboid(box, GizmoStyle.stroke(t.color(), 3f));
			}
		}
	}
}
