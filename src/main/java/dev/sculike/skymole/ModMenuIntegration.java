package dev.sculike.skymole;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.sculike.skymole.ui.MenuScreen;

/** Makes the config button next to SkyMole in Mod Menu open the SkyMole menu. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return MenuScreen::new;
	}
}
