package dev.sculike.skymole.hud;

import java.util.List;
import java.util.function.Supplier;

import dev.sculike.skymole.config.SkyMoleConfig;

/**
 * One movable HUD panel. {@code rows} returns null when the panel shouldn't show right
 * now (e.g. not on a mining island); {@code preview} is example data for the HUD editor.
 */
public record HudElement(
		String name,
		Supplier<SkyMoleConfig.Hud> hud,
		Supplier<String> title,
		Supplier<List<HudPanel.Row>> rows,
		Supplier<List<HudPanel.Row>> preview) {
}
