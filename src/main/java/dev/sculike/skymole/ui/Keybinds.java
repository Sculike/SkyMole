package dev.sculike.skymole.ui;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** SkyMole's keys. Rebind them in Options > Controls, under Miscellaneous. */
public final class Keybinds {
	private static KeyMapping openMenu;

	private Keybinds() {}

	public static void register() {
		openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.skymole.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_M, KeyMapping.Category.MISC));
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		if (openMenu == null) return;
		boolean pressed = false;
		while (openMenu.consumeClick()) pressed = true;
		// Only when nothing else is open, so the key can't interrupt a Hypixel menu.
		if (pressed && mc.gui.screen() == null) mc.gui.setScreen(new MenuScreen());
	}

	/** The key it's bound to right now (e.g. "M"), for messages. */
	public static Component boundKey() {
		return openMenu == null ? Component.literal("M") : openMenu.getTranslatedKeyMessage();
	}
}
