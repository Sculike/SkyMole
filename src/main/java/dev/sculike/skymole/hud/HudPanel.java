package dev.sculike.skymole.hud;

import java.util.List;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a compact SkyMole HUD panel: dark warm background, gold bar on the left,
 * gold title, then rows of "label  [progress bar]  value".
 */
public final class HudPanel {
	/**
	 * @param progress 0..1 to draw a progress bar, or -1 for none
	 */
	public record Row(String label, String value, int valueColor, float progress) {
		public static Row text(String label) {
			return new Row(label, "", Branding.TEXT_DIM, -1);
		}
	}

	static final int PAD = 3;
	private static final int BAR = 2;
	private static final int GAP = 6;
	private static final int PROGRESS_W = 34;
	private static final int TRACK = 0xFF3A2E24;

	private HudPanel() {}

	/** Where a panel of this size ends up on screen: {x, y}, anchored and kept inside the screen. */
	public static int[] position(GuiGraphicsExtractor g, SkyMoleConfig.Hud hud, int width, int height) {
		int x = hud.right ? g.guiWidth() - hud.x - width : hud.x;
		x = Math.max(0, Math.min(g.guiWidth() - width, x));
		int y = Math.max(0, Math.min(g.guiHeight() - height, hud.y));
		return new int[] {x, y};
	}

	/** Draws the panel and returns where it is on screen: {x, y, width, height} (after scaling). */
	public static int[] draw(GuiGraphicsExtractor g, SkyMoleConfig.Hud hud, String title, List<Row> rows) {
		Font font = Minecraft.getInstance().font;
		SkyMoleConfig.General general = SkyMoleConfig.get().general;
		boolean shadow = general.textShadow;

		int labelWidth = font.width(title);
		int valueWidth = 0;
		boolean anyBar = false;
		for (Row row : rows) {
			labelWidth = Math.max(labelWidth, font.width(row.label()));
			valueWidth = Math.max(valueWidth, font.width(row.value()));
			anyBar |= row.progress() >= 0;
		}
		int barSpace = anyBar ? GAP + PROGRESS_W : 0;
		int valueSpace = valueWidth > 0 ? GAP + valueWidth : 0;
		int innerWidth = labelWidth + barSpace + valueSpace;
		int width = BAR + PAD + innerWidth + PAD;
		int lineHeight = font.lineHeight;
		int height = PAD + lineHeight * (rows.size() + 1) + PAD - 1;

		float scale = hud.scalePercent / 100f;
		int[] pos = position(g, hud, Math.round(width * scale), Math.round(height * scale));
		g.pose().pushMatrix();
		g.pose().translate(pos[0], pos[1]);
		g.pose().scale(scale, scale);

		int alpha = Math.round(general.backgroundOpacityPercent / 100f * 255f);
		int background = (alpha << 24) | (Branding.PANEL_BG & 0xFFFFFF);
		g.fill(0, 0, width, height, background);
		g.fill(0, 0, BAR, height, Branding.LAMP_GOLD);

		int textX = BAR + PAD;
		int y = PAD;
		g.text(font, title, textX, y, Branding.LAMP_GOLD, shadow);
		for (Row row : rows) {
			y += lineHeight;
			// Text-only rows (no value, no bar) are hints/notes: draw them in their own color.
			boolean textOnly = row.value().isEmpty() && row.progress() < 0;
			g.text(font, row.label(), textX, y, textOnly ? row.valueColor() : Branding.TEXT, shadow);
			if (row.progress() >= 0) {
				int bx = textX + labelWidth + GAP;
				int by = y + 3;
				int filled = Math.round(Math.max(0, Math.min(1, row.progress())) * PROGRESS_W);
				g.fill(bx, by, bx + PROGRESS_W, by + 3, TRACK);
				g.fill(bx, by, bx + filled, by + 3, row.progress() >= 1 ? Branding.GOOD : Branding.LAMP_GOLD);
			}
			if (!row.value().isEmpty()) {
				int valueX = textX + innerWidth - font.width(row.value());
				g.text(font, row.value(), valueX, y, row.valueColor(), shadow);
			}
		}
		g.pose().popMatrix();
		return new int[] {pos[0], pos[1], Math.round(width * scale), Math.round(height * scale)};
	}
}
