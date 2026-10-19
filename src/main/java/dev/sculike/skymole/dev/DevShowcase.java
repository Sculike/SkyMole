package dev.sculike.skymole.dev;

import dev.sculike.skymole.SkyMole;
import dev.sculike.skymole.hud.HudEditorScreen;
import dev.sculike.skymole.ui.MenuScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Development only (`gradlew runShowcase`): opens the menu on each tab, takes a screenshot
 * of each into run/screenshots, then quits. Never active in a normal game.
 */
public final class DevShowcase {
	/** Menu tabs to screenshot; -1 means the HUD editor. */
	private static final int[] TABS = {0, 1, 3, 7, -1};
	private static int ticks = -1;

	private DevShowcase() {}

	public static boolean enabled() {
		return FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("skymole.showcase");
	}

	public static void tick(Minecraft mc) {
		if (ticks < 0) {
			// Wait until the title screen is up and the loading overlay is gone.
			if (!(mc.gui.screen() instanceof TitleScreen) || mc.gui.overlay() != null) return;
			ticks = 0;
		}
		ticks++;
		// Every 40 ticks (2 s): open the next tab, screenshot it 20 ticks later.
		int step = ticks / 40;
		int phase = ticks % 40;
		if (step < TABS.length) {
			if (phase == 1) mc.gui.setScreen(TABS[step] < 0 ? new HudEditorScreen(null) : MenuScreen.onTab(TABS[step]));
			if (phase == 21) {
				Screenshot.grab(mc, true);
				SkyMole.LOGGER.info("Showcase screenshot taken for tab {}", TABS[step]);
			}
		} else if (phase == 1) {
			mc.stop();
		}
	}
}
