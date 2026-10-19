package dev.sculike.skymole;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The SkyMole brand palette, taken from the icon (see branding/brand-sheet.html).
 * All colors are ARGB: Minecraft needs the alpha byte when drawing.
 */
public final class Branding {
	public static final String MOD_ID = "skymole";
	public static final String NAME = "SkyMole";

	// Social links shown in the menu footer. Empty = not set up yet.
	public static final String DISCORD_URL = "";
	public static final String YOUTUBE_URL = "";
	public static final String TIKTOK_URL = "";

	// Core brand
	public static final int LAMP_GOLD = 0xFFFFC93C;   // primary accent: HUD titles, chat prefix
	public static final int SNOUT_PINK = 0xFFF29BB0;  // signature secondary, use sparingly
	public static final int FUR = 0xFF4A4A55;
	public static final int FUR_LIGHT = 0xFF62626E;
	public static final int DIRT = 0xFF6A4628;
	public static final int DIRT_DEEP = 0xFF5A3A20;
	public static final int TUNNEL = 0xFF1A0F08;      // darkest, outlines

	// UI
	public static final int PANEL_BG = 0xCC1E1712;   // warm near-black, 80% opacity
	public static final int TEXT = 0xFFF4EDE4;
	public static final int TEXT_DIM = 0xFFB5A797;

	// Status (kept clearly apart from the gold accent)
	public static final int GOOD = 0xFF7BD88F;
	public static final int WARN = 0xFFFF7A45;
	public static final int BAD = 0xFFFF5C5C;
	public static final int MITHRIL = 0xFF4FC9DE;     // info, powder, mithril things

	private Branding() {}

	/** Chat prefix: "SkyMole »" in lamp gold. */
	public static MutableComponent prefix() {
		return Component.literal(NAME).withColor(LAMP_GOLD & 0xFFFFFF)
				.append(Component.literal(" » ").withColor(TEXT_DIM & 0xFFFFFF));
	}

	/** A chat line from the mod, like: SkyMole » Commission done! */
	public static MutableComponent chat(String message) {
		return prefix().append(Component.literal(message).withColor(TEXT & 0xFFFFFF));
	}
}
