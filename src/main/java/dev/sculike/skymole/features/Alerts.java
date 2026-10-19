package dev.sculike.skymole.features;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Every SkyMole alert looks and sounds the same: gold title, subtitle, chat line, sound. */
public final class Alerts {
	private Alerts() {}

	public static void show(String title, String subtitle) {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.hud.setTimes(5, 40, 10);
		mc.gui.hud.setTitle(Component.literal(title).withColor(Branding.LAMP_GOLD & 0xFFFFFF));
		mc.gui.hud.setSubtitle(Component.literal(subtitle).withColor(Branding.TEXT & 0xFFFFFF));
		chat(title + (subtitle.isEmpty() ? "" : " " + subtitle));
		SkyMoleConfig.General general = SkyMoleConfig.get().general;
		AlertSounds.play(general.alertSound, general.volumePercent);
	}

	public static void chat(String message) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Branding.chat(message));
	}
}
