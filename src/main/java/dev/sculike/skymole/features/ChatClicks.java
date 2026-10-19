package dev.sculike.skymole.features;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.SkyBlockState.Commission;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * With chat open your mouse is free, so the commission HUD becomes clickable:
 * right-click a commission to mark where to do it (or the nearest Emissary when it's done).
 */
public final class ChatClicks {
	private ChatClicks() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
			if (!(screen instanceof ChatScreen)) return;

			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				if (event.button() != 1 || !SkyMoleConfig.get().markers.enabled) return true;
				Commission c = Commissions.at(event.x(), event.y());
				if (c == null) return true;
				Markers.forCommission(c.name(), c.done());
				return false; // handled: don't pass the click on to chat
			});

			ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
				if (!SkyMoleConfig.get().markers.enabled) return;
				Commission c = Commissions.at(mouseX, mouseY);
				if (c == null) return;
				String hint = c.done() ? "Right-click: go to an Emissary" : "Right-click: mark where to go";
				int w = mc.font.width(hint) + 8;
				int x = Math.min(mouseX + 10, s.width - w - 2);
				g.fill(x, mouseY - 14, x + w, mouseY - 2, 0xE01E1712);
				g.text(mc.font, hint, x + 4, mouseY - 12, Branding.LAMP_GOLD, false);
			});
		});
	}
}
