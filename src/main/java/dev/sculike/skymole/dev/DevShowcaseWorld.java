package dev.sculike.skymole.dev;

import dev.sculike.skymole.SkyMole;
import dev.sculike.skymole.features.Markers;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;

/**
 * Development only (`gradlew runShowcaseWorld`): in a test world, places a marker in front
 * of the player, screenshots it, then quits. Never active in a normal game.
 */
public final class DevShowcaseWorld {
	private static int ticks;
	private static boolean creating;

	private DevShowcaseWorld() {}

	public static boolean enabled() {
		return FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("skymole.showcaseWorld");
	}

	public static void tick(Minecraft mc) {
		// On the title screen: create a fresh test world (Minecraft's own debug helper).
		if (!creating && mc.gui.screen() instanceof TitleScreen && mc.gui.overlay() == null) {
			creating = true;
			CreateWorldScreen.testWorld(mc, () -> { });
			return;
		}
		if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
		ticks++;
		if (ticks == 60) {
			// 20 blocks in front of where the player looks, at feet height.
			double yaw = Math.toRadians(mc.player.getYRot());
			BlockPos pos = mc.player.blockPosition().offset((int) Math.round(-Math.sin(yaw) * 20), 0,
					(int) Math.round(Math.cos(yaw) * 20));
			Markers.devMark("Royal Mines", pos);
			SkyMole.LOGGER.info("Showcase marker at {}", pos);
		}
		if (ticks == 120) {
			Screenshot.grab(mc, true);
			SkyMole.LOGGER.info("Showcase world screenshot taken");
		}
		if (ticks == 140) mc.stop();
	}
}
