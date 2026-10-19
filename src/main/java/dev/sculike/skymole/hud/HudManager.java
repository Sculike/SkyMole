package dev.sculike.skymole.hud;

import java.util.ArrayList;
import java.util.List;

import dev.sculike.skymole.config.SkyMoleConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** All SkyMole HUD panels, drawn together as one HUD layer. */
public final class HudManager {
	private static final List<HudElement> ELEMENTS = new ArrayList<>();
	/** Where each panel was drawn last frame, {x, y, width, height}, for clicks with chat open. */
	private static final java.util.Map<String, int[]> LAST_BOX = new java.util.HashMap<>();

	/** Shows every panel with example data anywhere (/skymole preview). */
	private static boolean preview;

	private HudManager() {}

	public static void add(HudElement element) {
		ELEMENTS.add(element);
	}

	public static List<HudElement> elements() {
		return ELEMENTS;
	}

	public static boolean togglePreview() {
		preview = !preview;
		return preview;
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LAST_BOX.clear();
		if (mc.gui.hud.isHidden() || mc.gui.screen() instanceof HudEditorScreen) return;
		for (HudElement e : ELEMENTS) {
			SkyMoleConfig.Hud hud = e.hud().get();
			if (!hud.enabled) continue;
			List<HudPanel.Row> rows = preview ? e.preview().get() : e.rows().get();
			if (rows == null || rows.isEmpty()) continue;
			LAST_BOX.put(e.name(), HudPanel.draw(g, hud, e.title().get(), rows));
		}
	}

	/**
	 * Which row of a panel is at this screen position: 0 = first row under the title,
	 * -1 = the title or outside the panel.
	 */
	public static int rowAt(String panel, SkyMoleConfig.Hud hud, double mouseX, double mouseY) {
		int[] box = LAST_BOX.get(panel);
		if (box == null || mouseX < box[0] || mouseX >= box[0] + box[2] || mouseY < box[1] || mouseY >= box[1] + box[3]) return -1;
		float scale = hud.scalePercent / 100f;
		double localY = (mouseY - box[1]) / scale - HudPanel.PAD;
		return (int) Math.floor(localY / Minecraft.getInstance().font.lineHeight) - 1;
	}
}
