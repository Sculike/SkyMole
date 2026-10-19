package dev.sculike.skymole.features;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.MiningIsland;
import dev.sculike.skymole.hypixel.Ores;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.SpreadStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A short mining route for Mining Spread: the GREEN block is the best one to mine now (most
 * same-ore neighbors, which Mining Spread can break too), the ORANGE block is the best one after
 * that. Green stays until you break it; then orange becomes green and a new orange is picked.
 * Only blocks you can actually see are used; nothing is shown through walls.
 */
public final class BestBlock {
	private static final double REACH = 5.0;
	/** Green is the aimed block or one right next to it (3x3x3); orange is 2 blocks from green. */
	private static final int AREA = 1;
	/** When spread broke both green and orange, the route continues within this many blocks of green. */
	private static final int CONTINUE_AREA = 2;
	/** Aiming further than this from green (same ore) restarts the route where you're looking. */
	private static final int FOLLOW_DISTANCE = 2;
	/** cos(35 degrees): new picks must be within 35 degrees of your crosshair. */
	private static final double MIN_VIEW_DOT = 0.82;
	private static final int RESCAN_TICKS = 4;
	private static final int ORANGE = 0xFFFF9F40;

	private static String ore;
	private static BlockPos green;
	private static BlockPos orange;
	private static double greenExpected;
	private static int ticks;
	private static boolean spreadWarned;
	private static MiningIsland warnedIsland;

	private BestBlock() {}

	/**
	 * Expected blocks broken by mining a block with this many same-ore neighbors: the block itself
	 * plus about one extra neighbor per 100 Mining Spread, but never more neighbors than it has.
	 */
	static double expectedBlocks(int neighbors, double spread) {
		return 1 + Math.min(neighbors, Math.max(0, spread) / 100.0);
	}

	/**
	 * The route works without knowing your spread (it ranks by neighbors); only the "x" label
	 * needs it. If SkyMole has never seen it, give a calm one-time tip on how to fix that.
	 */
	private static void checkSpreadKnown() {
		if (ore == null || SkyBlockState.sections().isEmpty()) return; // nothing aimed yet / tab not loaded
		if (SkyBlockState.island() != warnedIsland) {
			warnedIsland = SkyBlockState.island();
			spreadWarned = false;
		}
		if (spreadWarned || SpreadStats.forOre(ore) >= 0) return;
		spreadWarned = true;
		String stat = Ores.isGemstone(ore) ? "Gemstone Spread" : "Mining Spread";
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
				Branding.chat("Tip: SkyMole doesn't know your " + stat + " yet, so the x-label assumes 0. "
						+ "Open SkyBlock Menu > Your SkyBlock Profile once and it's saved, or set it in /skymole > Tools. ")
						.append(Component.literal("[Open SkyBlock Menu]").withColor(Branding.LAMP_GOLD & 0xFFFFFF)
								.withStyle(style -> style
										.withClickEvent(new ClickEvent.RunCommand("/sbmenu"))
										.withHoverEvent(new HoverEvent.ShowText(Component.literal(
												"Then open Your SkyBlock Profile"))))));
	}

	private static String oreAt(ClientLevel level, BlockPos pos) {
		return Ores.typeOf(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath());
	}

	private static void reset() {
		ore = null;
		green = null;
		orange = null;
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		SkyMoleConfig.Tools config = SkyMoleConfig.get().tools;
		if (!config.bestBlock || mc.player == null || mc.level == null || !SkyBlockState.onMiningIsland()) {
			reset();
			return;
		}
		if (++ticks >= RESCAN_TICKS) {
			ticks = 0;
			update(mc.level, mc.player, mc.hitResult);
			checkSpreadKnown();
		}
		draw(mc, config.bestBlockSecond);
	}

	private static void update(ClientLevel level, Player player, HitResult hit) {
		BlockPos aimed = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b.getBlockPos() : null;
		String aimedOre = aimed == null ? null : oreAt(level, aimed);

		// Aiming at a different ore starts a new route for that ore.
		if (aimedOre != null && !aimedOre.equals(ore)) {
			ore = aimedOre;
			green = null;
			orange = null;
		}
		// Aiming somewhere else (same ore) restarts the route there, so it follows where you look.
		if (aimedOre != null && green != null && chebyshev(aimed, green) > FOLLOW_DISTANCE) {
			green = null;
			orange = null;
		}
		if (ore == null) return;

		Vec3 eye = player.getEyePosition();
		double spread = Math.max(0, SpreadStats.forOre(ore));
		if (green != null && !stillThere(level, player, eye, green)) {
			// Green is gone. If it was mined (no longer ore), orange takes over. If Mining Spread
			// broke orange too, continue from where green was with the best block that's left.
			BlockPos was = green;
			boolean mined = !ore.equals(oreAt(level, green));
			green = null;
			if (mined && orange != null && oreInReach(level, eye, orange)) green = orange;
			else if (mined) green = best(level, player, eye, was, spread, CONTINUE_AREA, false);
			orange = null;
		}

		BlockPos anchor = aimedOre != null ? aimed : green;
		if (anchor == null) return;
		if (green == null) green = best(level, player, eye, anchor, spread, AREA, true);
		if (green == null) {
			orange = null;
			return;
		}
		if (orange == null || orange.equals(green) || !oreInReach(level, eye, orange) || chebyshev(orange, green) > 2) {
			orange = bestAfter(level, player, eye, green, spread);
		}
		greenExpected = expectedBlocks(neighbors(level, green, Set.of()), spread);
	}

	private static boolean oreInReach(ClientLevel level, Vec3 eye, BlockPos pos) {
		return ore.equals(oreAt(level, pos)) && Vec3.atCenterOf(pos).distanceTo(eye) <= REACH;
	}

	/**
	 * How good a block is to mine: the extra blocks Mining Spread will break with it. With 216 spread
	 * that's about 2, so a block with 2 neighbors is as good as one with 5: the cap avoids walking
	 * further for neighbors spread can't use anyway.
	 */
	private static double value(int neighbors, double spread) {
		return spread > 0 ? Math.min(neighbors, Math.ceil(spread / 100.0)) : neighbors;
	}

	/** Distance in blocks along the biggest axis (neighbors, also diagonal, are 1). */
	private static int chebyshev(BlockPos a, BlockPos b) {
		return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
	}

	/** Green/orange stay as long as they're still this ore, in reach and visible (you may look around). */
	private static boolean stillThere(ClientLevel level, Player player, Vec3 eye, BlockPos pos) {
		return ore.equals(oreAt(level, pos)) && Vec3.atCenterOf(pos).distanceTo(eye) <= REACH && visible(level, player, eye, pos);
	}

	/** Same-ore neighbors of a block, not counting blocks in {@code gone} (mined before it). */
	private static int neighbors(ClientLevel level, BlockPos pos, Set<BlockPos> gone) {
		int n = 0;
		for (Direction d : Direction.values()) {
			BlockPos next = pos.relative(d);
			if (!gone.contains(next) && ore.equals(oreAt(level, next))) n++;
		}
		return n;
	}

	/**
	 * Green: a block within {@code area} of {@code anchor} that's in reach and visible (and in view
	 * if {@code inView}), with the most spread value; ties go to the one closest to your crosshair.
	 */
	private static BlockPos best(ClientLevel level, Player player, Vec3 eye, BlockPos anchor, double spread,
			int area, boolean inView) {
		Vec3 look = player.getLookAngle();
		BlockPos bestPos = null;
		double bestValue = -1;
		double bestAngle = -2;
		for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-area, -area, -area), anchor.offset(area, area, area))) {
			Vec3 mid = Vec3.atCenterOf(pos);
			if (mid.distanceTo(eye) > REACH) continue;
			double angle = look.dot(mid.subtract(eye).normalize()); // 1 = straight ahead
			if (inView && angle < MIN_VIEW_DOT) continue;
			if (!ore.equals(oreAt(level, pos))) continue;
			if (!visible(level, player, eye, pos)) continue;
			double v = value(neighbors(level, pos, Set.of()), spread);
			if (v > bestValue || (v == bestValue && angle > bestAngle)) {
				bestPos = pos.immutable();
				bestValue = v;
				bestAngle = angle;
			}
		}
		return bestPos;
	}

	/**
	 * Orange: the best block to mine after green. Mining Spread usually breaks green's same-ore
	 * neighbors too, so orange is never one of those: it's one step further out, scored on the
	 * neighbors it will still have. It may be hidden now if it gets uncovered when green's
	 * neighbors break.
	 */
	private static BlockPos bestAfter(ClientLevel level, Player player, Vec3 eye, BlockPos green, double spread) {
		Set<BlockPos> gone = new HashSet<>();
		gone.add(green);
		for (Direction d : Direction.values()) {
			BlockPos n = green.relative(d);
			if (ore.equals(oreAt(level, n))) gone.add(n);
		}
		Vec3 look = player.getLookAngle();
		BlockPos bestPos = null;
		double bestValue = -1;
		double bestAngle = -2;
		for (BlockPos pos : BlockPos.betweenClosed(green.offset(-2, -2, -2), green.offset(2, 2, 2))) {
			if (gone.contains(pos)) continue;
			Vec3 mid = Vec3.atCenterOf(pos);
			if (mid.distanceTo(eye) > REACH) continue;
			if (!ore.equals(oreAt(level, pos))) continue;
			boolean uncovered = false;
			for (Direction d : Direction.values()) {
				if (gone.contains(pos.relative(d))) {
					uncovered = true;
					break;
				}
			}
			if (!uncovered && !visible(level, player, eye, pos)) continue;
			double v = value(neighbors(level, pos, gone), spread);
			double angle = look.dot(mid.subtract(eye).normalize());
			if (v > bestValue || (v == bestValue && angle > bestAngle)) {
				bestPos = pos.immutable();
				bestValue = v;
				bestAngle = angle;
			}
		}
		return bestPos;
	}

	/**
	 * True if you can actually see the block: it has an open face (nothing solid next to it)
	 * that faces you, and a line from your eyes to that face isn't blocked by anything.
	 */
	private static boolean visible(ClientLevel level, Player player, Vec3 eye, BlockPos pos) {
		Vec3 mid = Vec3.atCenterOf(pos);
		for (Direction d : Direction.values()) {
			BlockPos next = pos.relative(d);
			if (!level.getBlockState(next).getCollisionShape(level, next).isEmpty()) continue; // face is covered
			Vec3 normal = Vec3.atLowerCornerOf(d.getUnitVec3i());
			Vec3 face = mid.add(normal.scale(0.5));
			if (eye.subtract(face).dot(normal) <= 0) continue; // face points away from you
			// Aim just inside the face so the line ends in this block if nothing is in the way.
			Vec3 target = mid.add(normal.scale(0.45));
			BlockHitResult result = level.clip(new ClipContext(eye, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
			if (result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) return true;
		}
		return false;
	}

	/** A thick outline around the block, no fill. */
	private static GizmoStyle outline(int color) {
		return GizmoStyle.stroke(color, 6f);
	}

	private static void draw(Minecraft mc, boolean showOrange) {
		if (green == null) return;
		try (var ignored = mc.collectPerTickGizmos()) {
			// Slightly bigger than the block so the lines aren't hidden inside its edges.
			// No setAlwaysOnTop: hidden behind other blocks, like a normal block outline.
			Gizmos.cuboid(green, 0.01f, outline(Branding.GOOD));
			if (showOrange && orange != null) Gizmos.cuboid(orange, 0.01f, outline(ORANGE));
			String label = "x" + String.format(Locale.ROOT, "%.1f", greenExpected);
			Gizmos.billboardText(label, Vec3.atCenterOf(green).add(0, 0.8, 0),
					TextGizmo.Style.forColorAndCentered(Branding.GOOD).withScale(0.4f));
		}
	}

	/** For /skymole debug. */
	public static String describe() {
		return "ore=" + ore + " green=" + green + " orange=" + orange + " expected=" + greenExpected;
	}
}
