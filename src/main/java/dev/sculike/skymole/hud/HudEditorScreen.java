package dev.sculike.skymole.hud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Drag-and-drop HUD editor: every panel shows example data. Drag to move,
 * scroll to resize, right-click to reset, middle-click to hide, Esc to save and close.
 * Panels dropped on the right half stick to the right edge of the screen.
 */
public class HudEditorScreen extends Screen {
	private final Screen parent;
	/** Where each panel was drawn last frame: {x, y, width, height}. */
	private final Map<HudElement, int[]> boxes = new HashMap<>();
	private final SkyMoleConfig defaults = new SkyMoleConfig();

	private HudElement dragging;
	private double grabX, grabY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("SkyMole HUD editor"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		if (minecraft.level == null) super.extractBackground(g, mouseX, mouseY, partialTick);
		else g.fill(0, 0, width, height, 0x40000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);

		// Center line: panels dropped right of it stick to the right edge.
		g.fill(width / 2, 0, width / 2 + 1, height, 0x20FFFFFF);

		HudElement hovered = dragging != null ? dragging : elementAt(mouseX, mouseY);
		for (HudElement e : HudManager.elements()) {
			SkyMoleConfig.Hud hud = e.hud().get();
			int[] box = HudPanel.draw(g, hud, e.title().get(), e.preview().get());
			boxes.put(e, box);
			int color = e == hovered ? Branding.LAMP_GOLD : hud.enabled ? 0x80FFFFFF : 0x80FF5C5C;
			outline(g, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, color);
			if (!hud.enabled) g.text(font, "hidden", box[0] + box[2] - font.width("hidden") - 2, box[1] + 2, Branding.BAD, true);
		}

		String hint = "Drag: move · Scroll: size · Right-click: reset · Middle-click: hide · Esc: save";
		int hw = Math.min(width - 8, font.width(hint) + 12);
		int hx = (width - hw) / 2, hy = height - 18;
		g.fill(hx, hy, hx + hw, hy + 14, 0xE01E1712);
		g.text(font, hint, hx + 6, hy + 3, Branding.TEXT, false);
		if (hovered != null) {
			SkyMoleConfig.Hud hud = hovered.hud().get();
			String info = hovered.name() + "  " + hud.scalePercent + "%" + (hud.enabled ? "" : "  (hidden)");
			g.text(font, info, (width - font.width(info)) / 2, hy - 12, Branding.LAMP_GOLD, true);
		}
	}

	private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	private HudElement elementAt(double mx, double my) {
		List<HudElement> all = HudManager.elements();
		// Last drawn is on top, so search from the end.
		for (int i = all.size() - 1; i >= 0; i--) {
			int[] b = boxes.get(all.get(i));
			if (b != null && mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) return all.get(i);
		}
		return null;
	}

	private SkyMoleConfig.Hud defaultsFor(HudElement e) {
		return switch (e.name()) {
			case "Commissions" -> defaults.commissions.hud;
			case "Powder" -> defaults.powder.hud;
			case "Events" -> defaults.events.hud;
			case "Tools" -> defaults.tools.hud;
			case "Marker" -> defaults.markers.hud;
			default -> defaults.area.hud;
		};
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		HudElement e = elementAt(event.x(), event.y());
		if (e == null) return super.mouseClicked(event, doubleClick);
		SkyMoleConfig.Hud hud = e.hud().get();
		switch (event.button()) {
			case 0 -> {
				int[] b = boxes.get(e);
				dragging = e;
				grabX = event.x() - b[0];
				grabY = event.y() - b[1];
			}
			case 1 -> {
				SkyMoleConfig.Hud def = defaultsFor(e);
				hud.moveTo(def);
				hud.scalePercent = def.scalePercent;
			}
			case 2 -> hud.enabled = !hud.enabled;
			default -> { }
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(event, dx, dy);
		SkyMoleConfig.Hud hud = dragging.hud().get();
		int[] b = boxes.get(dragging);
		int w = b[2], h = b[3];
		int x = (int) Math.max(0, Math.min(width - w, Math.round(event.x() - grabX)));
		int y = (int) Math.max(0, Math.min(height - h, Math.round(event.y() - grabY)));
		hud.right = x + w / 2 > width / 2;
		hud.x = hud.right ? width - x - w : x;
		hud.y = y;
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		HudElement e = elementAt(mouseX, mouseY);
		if (e == null) return false;
		SkyMoleConfig.Hud hud = e.hud().get();
		hud.scalePercent = (int) Math.max(50, Math.min(300, hud.scalePercent + Math.signum(scrollY) * 10));
		return true;
	}

	@Override
	public void onClose() {
		SkyMoleConfig.save();
		minecraft.gui.setScreen(parent);
	}
}
