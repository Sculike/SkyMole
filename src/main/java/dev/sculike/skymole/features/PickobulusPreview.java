package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.Ores;
import dev.sculike.skymole.hypixel.SkyBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * When Pickobulus is ready and you hold a mining tool: outlines exactly the blocks the blast will
 * break around the block you aim at, with the count. The blast shape (radius, all blocks or ores
 * only) calibrates itself: when you throw, SkyMole counts several possible shapes, and when
 * Hypixel says "Your Pickobulus destroyed 34 blocks!" it keeps the shape that matched.
 * Outlines are hidden behind other blocks, so nothing shows through walls.
 */
public final class PickobulusPreview {
	/** A possible blast shape. */
	record Shape(double radius, boolean oresOnly) {}

	static final List<Shape> SHAPES = List.of(
			new Shape(2.0, false), new Shape(2.5, false), new Shape(3.0, false), new Shape(1.5, false),
			new Shape(2.0, true), new Shape(2.5, true), new Shape(3.0, true));

	private static final double RANGE = 20;
	private static final int SCAN_TICKS = 3;
	private static final int COLOR = Branding.LAMP_GOLD;
	private static final Pattern USED = Pattern.compile("^You used your Pickobulus Pickaxe Ability!");
	private static final Pattern DESTROYED = Pattern.compile("^Your Pickobulus destroyed ([\\d,]+) blocks?!");

	private static BlockPos landing;
	private static List<BlockPos> hits = List.of();
	private static int ticks;
	/** Block counts for each shape at the moment you threw, to compare with Hypixel's number. */
	private static int[] countsAtThrow;

	private PickobulusPreview() {}

	public static void register() {
		Chat.listen(text -> {
			if (USED.matcher(text).find() && landing != null) {
				ClientLevel level = Minecraft.getInstance().level;
				if (level == null) return;
				countsAtThrow = new int[SHAPES.size()];
				for (int i = 0; i < SHAPES.size(); i++) countsAtThrow[i] = blocksIn(level, landing, SHAPES.get(i)).size();
				return;
			}
			Matcher m = DESTROYED.matcher(text);
			if (m.find() && countsAtThrow != null) {
				calibrate(Integer.parseInt(m.group(1).replace(",", "")), countsAtThrow);
				countsAtThrow = null;
			}
		});
	}

	/** Keeps the shape whose count matched Hypixel's; the current one wins ties. */
	static Shape bestShape(int destroyed, int[] counts, Shape current) {
		Shape best = current;
		int bestDiff = Integer.MAX_VALUE;
		for (int i = 0; i < SHAPES.size(); i++) {
			if (SHAPES.get(i).equals(current)) bestDiff = Math.abs(counts[i] - destroyed);
		}
		for (int i = 0; i < SHAPES.size(); i++) {
			int diff = Math.abs(counts[i] - destroyed);
			if (diff < bestDiff) {
				bestDiff = diff;
				best = SHAPES.get(i);
			}
		}
		return best;
	}

	private static void calibrate(int destroyed, int[] counts) {
		SkyMoleConfig.Tools t = SkyMoleConfig.get().tools;
		Shape current = new Shape(t.pickobulusRadius, t.pickobulusOresOnly);
		Shape best = bestShape(destroyed, counts, current);
		if (best.equals(current)) return;
		t.pickobulusRadius = best.radius();
		t.pickobulusOresOnly = best.oresOnly();
		SkyMoleConfig.save();
		Alerts.chat(String.format(Locale.ROOT, "Pickobulus preview calibrated: radius %.1f, %s.",
				best.radius(), best.oresOnly() ? "ores only" : "all blocks"));
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		SkyMoleConfig.Tools config = SkyMoleConfig.get().tools;
		if (!config.pickobulusPreview || mc.player == null || mc.level == null || !SkyBlockState.onMiningIsland()
				|| !Tools.holdingMiningTool() || !Tools.isAbilityReady("Pickobulus")) {
			hits = List.of();
			return; // keep "landing" so the throw can still be measured
		}
		if (++ticks >= SCAN_TICKS) {
			ticks = 0;
			scan(mc.level, mc.player, new Shape(config.pickobulusRadius, config.pickobulusOresOnly));
		}
		draw(mc);
	}

	private static void scan(ClientLevel level, Player player, Shape shape) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			landing = null;
			hits = List.of();
			return;
		}
		landing = hit.getBlockPos();
		hits = blocksIn(level, landing, shape);
	}

	/** The blocks this shape breaks around the impact block (a ball, measured between block centers). */
	static List<BlockPos> blocksIn(ClientLevel level, BlockPos center, Shape shape) {
		List<BlockPos> out = new ArrayList<>();
		int r = (int) Math.ceil(shape.radius());
		Vec3 c = Vec3.atCenterOf(center);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (Vec3.atCenterOf(pos).distanceTo(c) > shape.radius() + 1e-6) continue;
			BlockState state = level.getBlockState(pos);
			if (!breakable(state)) continue;
			if (shape.oresOnly() && Ores.typeOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()) == null) continue;
			out.add(pos.immutable());
		}
		return out;
	}

	/** Air, bedrock (already mined spots) and barriers don't break. */
	private static boolean breakable(BlockState state) {
		if (state.isAir()) return false;
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		return !id.equals("bedrock") && !id.equals("barrier") && !id.endsWith("water") && !id.endsWith("lava");
	}

	private static void draw(Minecraft mc) {
		if (landing == null || hits.isEmpty()) return;
		try (var ignored = mc.collectPerTickGizmos()) {
			// Depth-tested: blocks inside the rock are counted, but their outlines stay hidden by it.
			for (BlockPos p : hits) Gizmos.cuboid(p, 0.01f, GizmoStyle.stroke(COLOR, 2.5f));
			Gizmos.billboardText("Pickobulus: " + hits.size() + " blocks", Vec3.atCenterOf(landing).add(0, 3.2, 0),
					TextGizmo.Style.forColorAndCentered(COLOR).withScale(0.5f));
		}
	}
}
