package dev.sculike.skymole.ui;

import java.util.ArrayList;
import java.util.List;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hud.HudEditorScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** The SkyMole settings menu, opened with /skymole. */
public class MenuScreen extends Screen {
	private record Hitbox(MenuEntry entry, int x, int y, int width, int height) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + width && my >= y && my < y + height;
		}
	}

	private record Social(String name, int color, String url) {}

	private static final Social[] SOCIALS = {
			new Social("Discord", 0xFF5865F2, Branding.DISCORD_URL),
			new Social("YouTube", 0xFFFF0033, Branding.YOUTUBE_URL),
			new Social("TikTok", 0xFF25F4EE, Branding.TIKTOK_URL)};

	private static final Identifier ICON = Identifier.fromNamespaceAndPath(Branding.MOD_ID, "icon.png");
	private static final int WINDOW_BG = 0xF51E1712;
	private static final int SIDEBAR_BG = 0xFF19130F;
	private static final int LINE = 0xFF3A2C20;
	private static final int LINE_STRONG = 0xFF5A4430;
	private static final int HEADER_H = 34, SIDE_W = 112, SIDE_ROW_H = 20, STRIP_H = 3, FOOTER_H = 16, PAD = 10;

	/** Remembered while the game runs, so reopening shows the same tab. */
	private static int lastTab;

	private final List<MenuTab> tabs;
	private final List<Hitbox> hitboxes = new ArrayList<>();
	private final int[] socialX = new int[SOCIALS.length];
	private final int[] socialW = new int[SOCIALS.length];
	private int current = lastTab;
	private double scroll;
	private int contentHeight;

	private MenuEntry.Slider dragging;
	private int dragX, dragWidth;

	// Window bounds, set in init()
	private int left, top, winW, winH;

	/** Where Esc goes back to (Mod Menu), or null to close. */
	private final Screen parent;

	public MenuScreen() {
		this(null);
	}

	public MenuScreen(Screen parent) {
		super(Component.literal(Branding.NAME));
		this.parent = parent;
		tabs = MenuTabs.build(() -> {
			SkyMoleConfig.save();
			lastTab = current;
			minecraft.gui.setScreen(new HudEditorScreen(this));
		});
		if (current >= tabs.size()) current = 0;
	}

	/** Opens the menu on a specific tab (0 = first). */
	public static MenuScreen onTab(int tab) {
		lastTab = tab;
		return new MenuScreen();
	}

	@Override
	protected void init() {
		winW = Math.min(500, width - 16);
		winH = Math.min(320, height - 16);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
		clampScroll();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		SkyMoleConfig.save();
		lastTab = current;
		minecraft.gui.setScreen(parent);
	}

	// ---- Layout helpers --------------------------------------------------------

	private int bodyTop() { return top + HEADER_H; }
	private int bodyBottom() { return top + winH - FOOTER_H - STRIP_H; }
	private int contentLeft() { return left + SIDE_W; }

	/** Sidebar rows shrink on small screens so every category stays visible. */
	private int sideRowH() {
		return Math.max(12, Math.min(SIDE_ROW_H, (bodyBottom() - bodyTop() - 6) / tabs.size()));
	}

	private void clampScroll() {
		int visible = bodyBottom() - bodyTop() - 8;
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - visible)));
	}

	// ---- Drawing ---------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		if (minecraft.level == null) {
			super.extractBackground(g, mouseX, mouseY, partialTick); // title screen: normal panorama
			return;
		}
		switch (SkyMoleConfig.get().general.menuBackground) {
			case 0 -> g.fill(0, 0, width, height, 0x30000000); // light tint, the game stays visible
			case 1 -> super.extractBackground(g, mouseX, mouseY, partialTick);
			default -> { }
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);

		g.fill(left - 2, top - 2, left + winW + 2, top + winH + 2, 0x60000000);
		g.fill(left - 1, top - 1, left + winW + 1, top + winH + 1, LINE_STRONG);
		g.fill(left, top, left + winW, top + winH, WINDOW_BG);

		drawHeader(g);
		drawSidebar(g, mouseX, mouseY);
		drawContent(g, mouseX, mouseY);
		drawFooter(g, mouseX, mouseY);
	}

	private void drawHeader(GuiGraphicsExtractor g) {
		int iconSize = 24;
		int ix = left + PAD, iy = top + (HEADER_H - iconSize) / 2;
		g.fill(ix - 1, iy - 1, ix + iconSize + 1, iy + iconSize + 1, Branding.LAMP_GOLD);
		g.blit(RenderPipelines.GUI_TEXTURED, ICON, ix, iy, 0, 0, iconSize, iconSize, 128, 128, 128, 128);

		int tx = ix + iconSize + 8;
		g.pose().pushMatrix();
		g.pose().translate(tx, top + 7);
		g.pose().scale(1.5f, 1.5f);
		g.text(font, Branding.NAME, 0, 0, Branding.LAMP_GOLD, true);
		g.pose().popMatrix();
		g.text(font, "Mining helper for SkyBlock", tx, top + 21, Branding.TEXT_DIM, false);

		String version = "v" + FabricLoader.getInstance().getModContainer(Branding.MOD_ID)
				.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
		g.text(font, version, left + winW - PAD - font.width(version), top + 9, Branding.TEXT_DIM, false);
		String by = "by sculike";
		g.text(font, by, left + winW - PAD - font.width(by), top + 20, Branding.SNOUT_PINK, false);

		g.fill(left, bodyTop() - 1, left + winW, bodyTop(), LINE);
	}

	private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = left, y = bodyTop();
		g.fill(x, y, x + SIDE_W, bodyBottom(), SIDEBAR_BG);
		g.fill(x + SIDE_W - 1, y, x + SIDE_W, bodyBottom(), LINE);
		int rowH = sideRowH();
		for (int i = 0; i < tabs.size(); i++) {
			MenuTab tab = tabs.get(i);
			int ry = y + 4 + i * rowH;
			boolean active = i == current;
			boolean hover = mouseX >= x && mouseX < x + SIDE_W - 1 && mouseY >= ry && mouseY < ry + rowH;
			if (active) {
				g.fill(x, ry, x + SIDE_W - 1, ry + rowH, 0x24FFC93C);
				g.fill(x, ry, x + 2, ry + rowH, Branding.LAMP_GOLD);
			} else if (hover) {
				g.fill(x, ry, x + SIDE_W - 1, ry + rowH, 0x10FFFFFF);
			}
			ItemStack icon = tab.icon();
			int textX = x + 8;
			if (icon != null) {
				g.pose().pushMatrix();
				g.pose().translate(x + 7, ry + (rowH - 12) / 2f);
				g.pose().scale(0.75f, 0.75f);
				g.item(icon, 0, 0);
				g.pose().popMatrix();
				textX = x + 23;
			}
			int color = active ? Branding.LAMP_GOLD : hover ? Branding.TEXT : Branding.TEXT_DIM;
			g.text(font, tab.name, textX, ry + (rowH - 8) / 2, color, false);
		}
	}

	private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = contentLeft() + PAD;
		int w = left + winW - x - PAD - 4;
		int cTop = bodyTop(), cBottom = bodyBottom();

		hitboxes.clear();
		contentHeight = 0;
		for (MenuEntry e : tabs.get(current).entries) contentHeight += e.height();
		contentHeight += 8;
		clampScroll();

		g.enableScissor(contentLeft(), cTop, left + winW, cBottom);
		int y = cTop + 2 - (int) scroll;
		boolean mouseInContent = mouseX >= contentLeft() && mouseY >= cTop && mouseY < cBottom;
		for (MenuEntry e : tabs.get(current).entries) {
			int h = e.height();
			Hitbox box = new Hitbox(e, x, y, w, h);
			boolean hover = mouseInContent && box.contains(mouseX, mouseY)
					&& !(e instanceof MenuEntry.Header) && !(e instanceof MenuEntry.Description);
			if (y + h > cTop && y < cBottom) e.draw(g, font, x, y, w, hover);
			hitboxes.add(box);
			y += h;
		}
		g.disableScissor();

		int visible = cBottom - cTop - 8;
		if (contentHeight > visible) {
			int trackX = left + winW - 6;
			g.fill(trackX, cTop + 4, trackX + 2, cBottom - 4, 0xFF120D09);
			int trackH = cBottom - cTop - 8;
			int thumbH = Math.max(16, trackH * visible / contentHeight);
			int thumbY = cTop + 4 + (int) ((trackH - thumbH) * (scroll / (contentHeight - visible)));
			g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, LINE_STRONG);
		}
	}

	private void drawFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int stripY = bodyBottom();
		for (int sx = left, i = 0; sx < left + winW; sx += 8, i++) {
			g.fill(sx, stripY, Math.min(sx + 8, left + winW), stripY + STRIP_H, i % 2 == 0 ? Branding.DIRT : Branding.DIRT_DEEP);
		}
		int[] flecks = {winW / 12, winW * 3 / 10, winW * 6 / 10, winW * 17 / 20};
		for (int i = 0; i < flecks.length; i++) {
			int fx = left + flecks[i];
			g.fill(fx, stripY + 1, fx + 2, stripY + 2, i % 2 == 0 ? Branding.MITHRIL : Branding.LAMP_GOLD);
		}

		// Social links: small colored square + name, highlighted on hover
		int fy = stripY + STRIP_H + 4;
		int sx = left + PAD;
		for (int i = 0; i < SOCIALS.length; i++) {
			Social s = SOCIALS[i];
			if (s.url().isEmpty()) {
				// Only shown once there's a link (see Branding).
				socialX[i] = Integer.MIN_VALUE / 2;
				socialW[i] = 0;
				continue;
			}
			int w = 10 + font.width(s.name());
			socialX[i] = sx;
			socialW[i] = w;
			boolean hover = mouseX >= sx - 2 && mouseX < sx + w + 2 && mouseY >= fy - 2 && mouseY < fy + 10;
			g.fill(sx, fy + 1, sx + 6, fy + 7, s.color());
			g.text(font, s.name(), sx + 10, fy, hover ? Branding.TEXT : Branding.TEXT_DIM, false);
			sx += w + 14;
		}
		String help = "/skymole help";
		g.text(font, help, left + winW - PAD - font.width(help), fy, Branding.TEXT_DIM, false);
	}

	// ---- Input ----------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int button = event.button();

		// Sidebar
		if (mx >= left && mx < contentLeft() && my >= bodyTop() + 4 && my < bodyBottom()) {
			int i = (int) ((my - bodyTop() - 4) / sideRowH());
			if (i >= 0 && i < tabs.size() && i != current) {
				current = i;
				scroll = 0;
			}
			return true;
		}

		// Content
		if (mx >= contentLeft() && my >= bodyTop() && my < bodyBottom()) {
			for (Hitbox box : hitboxes) {
				if (!box.contains(mx, my)) continue;
				if (box.entry() instanceof MenuEntry.Slider slider && button == 0 && slider.grabs(mx, box.x(), box.width())) {
					dragging = slider;
					dragX = box.x();
					dragWidth = box.width();
				}
				box.entry().click(mx, button, box.x(), box.width());
				return true;
			}
		}

		// Footer social links
		int fy = bodyBottom() + STRIP_H + 4;
		if (button == 0 && my >= fy - 2 && my < fy + 10) {
			for (int i = 0; i < SOCIALS.length; i++) {
				if (socialW[i] > 0 && mx >= socialX[i] - 2 && mx < socialX[i] + socialW[i] + 2) {
					openSocial(SOCIALS[i]);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void openSocial(Social social) {
		if (social.url().isEmpty()) {
			minecraft.gui.hud.getChat().addClientSystemMessage(
					Branding.chat("The SkyMole " + social.name() + " is coming soon."));
			return;
		}
		ConfirmLinkScreen.confirmLinkNow(this, social.url());
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			dragging.drag(event.x(), dragX, dragWidth);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= scrollY * 12;
		clampScroll();
		return true;
	}
}
