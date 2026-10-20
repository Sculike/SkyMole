package dev.sculike.skymole.features;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.ui.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * A one-time hello the first time you join SkyBlock, so new users know SkyMole is there,
 * how to open it, and that it reads Hypixel's tab widgets.
 */
public final class Welcome {
	private Welcome() {}

	/** Hypixel spams chat when you join, so wait until that has passed. */
	private static final long DELAY_MS = 8_000;
	private static long joinedAt;

	/** Called after every state update. */
	public static void tick() {
		SkyMoleConfig config = SkyMoleConfig.get();
		if (config.general.welcomed) return;
		if (!SkyBlockState.inSkyBlock()) {
			joinedAt = 0;
			return;
		}
		long now = System.currentTimeMillis();
		if (joinedAt == 0) {
			joinedAt = now;
			return;
		}
		if (now - joinedAt < DELAY_MS) return;
		config.general.welcomed = true;
		SkyMoleConfig.save();

		ChatComponent chat = Minecraft.getInstance().gui.hud.getChat();
		chat.addClientSystemMessage(Branding.chat("Thanks for installing SkyMole! Press ")
				.append(Keybinds.boundKey().copy().withColor(Branding.LAMP_GOLD & 0xFFFFFF))
				.append(Component.literal(" or type ").withColor(Branding.TEXT & 0xFFFFFF))
				.append(button("/skymole", "/skymole", "Open the SkyMole menu"))
				.append(Component.literal(" for the settings.").withColor(Branding.TEXT & 0xFFFFFF)));
		chat.addClientSystemMessage(Branding.chat("It reads Hypixel's tab widgets, so turn on "
						+ "Commissions, Powders, Pickaxe Ability, Forge and Stats: ")
				.append(button("/widget", "/widget", "Turn the widgets on")));
	}

	/** A clickable gold command in chat. */
	private static MutableComponent button(String label, String command, String tooltip) {
		return Component.literal(label).withColor(Branding.LAMP_GOLD & 0xFFFFFF)
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal(tooltip))));
	}
}
