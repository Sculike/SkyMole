package dev.sculike.skymole.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.features.CorpseLoot.Reward;
import dev.sculike.skymole.hypixel.Prices;
import dev.sculike.skymole.hypixel.SkyBlockItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Optional on-screen scenes, drawn over the game without blocking it:
 * the corpse "case opening" roulette and the big-drop celebration. They play one after another.
 */
public final class Cutscenes {
	private interface Scene {
		/** Draws one frame; returns false when the scene is over. */
		boolean frame(GuiGraphicsExtractor g, Font font, float t);
	}

	private static final Deque<Scene> QUEUE = new ArrayDeque<>();
	private static Scene current;
	private static long startNanos;

	private Cutscenes() {}

	public static void roulette(String corpse, List<Reward> loot, Reward winner, boolean fast) {
		QUEUE.add(new Roulette(corpse, loot, winner, fast));
	}

	public static void bigDrop(String item, double value) {
		QUEUE.add(new BigDrop(item, value));
	}

	public static void render(GuiGraphicsExtractor g) {
		if (current == null) {
			current = QUEUE.poll();
			if (current == null) return;
			startNanos = System.nanoTime();
		}
		float t = (System.nanoTime() - startNanos) / 1e9f;
		if (!current.frame(g, Minecraft.getInstance().font, t)) current = null;
	}

	// ---- helpers ----------------------------------------------------------------

	private static int alpha(int argb, float a) {
		int base = (argb >>> 24) & 0xFF;
		int out = Math.round(base * Math.max(0, Math.min(1, a)));
		return (out << 24) | (argb & 0xFFFFFF);
	}

	private static float clamp01(float v) {
		return Math.max(0, Math.min(1, v));
	}

	private static float easeOutCubic(float x) {
		float p = 1 - clamp01(x);
		return 1 - p * p * p;
	}

	private static float easeOutBack(float x) {
		x = clamp01(x);
		float c1 = 1.70158f, c3 = c1 + 1;
		return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
	}

	/** Hue/saturation/brightness (0..1) to RGB, without loading Java's desktop (AWT) classes. */
	static int hsb(float h, float s, float b) {
		float r, gr, bl;
		float hh = (h - (float) Math.floor(h)) * 6f;
		float f = hh - (float) Math.floor(hh);
		float p = b * (1 - s), q = b * (1 - s * f), u = b * (1 - s * (1 - f));
		switch ((int) hh) {
			case 0 -> { r = b; gr = u; bl = p; }
			case 1 -> { r = q; gr = b; bl = p; }
			case 2 -> { r = p; gr = b; bl = u; }
			case 3 -> { r = p; gr = q; bl = b; }
			case 4 -> { r = u; gr = p; bl = b; }
			default -> { r = b; gr = p; bl = q; }
		}
		return (Math.round(r * 255) << 16) | (Math.round(gr * 255) << 8) | Math.round(bl * 255);
	}

	private static void play(SoundEvent sound, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	/** Rarity color by value: gray < green < blue < purple < gold < red. */
	static int rarity(double value) {
		if (value < 0) return 0xFF8A8F98;
		if (value >= 100_000_000) return 0xFFFF5577;
		if (value >= 10_000_000) return 0xFFFFC93C;
		if (value >= 5_000_000) return 0xFFC77DFF;
		if (value >= 1_000_000) return 0xFF4A7BFF;
		if (value >= 100_000) return 0xFF7BD88F;
		return 0xFFB0B0B0;
	}

	/** What a card shows as value: "1.25m", "powder" for powders (they have no price), "?" if unknown. */
	static String valueLabel(String name, double value) {
		if (value < 0 && name.toLowerCase(Locale.ROOT).contains("powder")) return "powder";
		return shortValue(value);
	}

	/** Draws an item icon centered at (cx, cy), {@code size} pixels big. */
	private static void icon(GuiGraphicsExtractor g, ItemStack stack, float cx, float cy, float size) {
		float scale = size / 16f;
		g.pose().pushMatrix();
		g.pose().translate(cx - size / 2, cy - size / 2);
		g.pose().scale(scale, scale);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}

	static String shortValue(double value) {
		if (value < 0) return "?";
		if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fb", value / 1e9);
		if (value >= 1_000_000) return String.format(Locale.ROOT, "%.2fm", value / 1e6);
		if (value >= 1_000) return String.format(Locale.ROOT, "%.1fk", value / 1e3);
		return String.valueOf(Math.round(value));
	}

	private static void text(GuiGraphicsExtractor g, Font font, String s, float cx, float y, float scale, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, y);
		g.pose().scale(scale, scale);
		g.text(font, s, -font.width(s) / 2, 0, color, true);
		g.pose().popMatrix();
	}

	// ---- the corpse roulette ----------------------------------------------------

	private static final class Roulette implements Scene {
		private record Card(String name, double value, ItemStack icon) {
			Card(String name, double value) {
				this(name, value, SkyBlockItems.icon(name));
			}
		}

		private static final String[] FILLER = {
				"Glacite Powder", "Umber", "Tungsten", "Glacite", "Enchanted Umber", "Enchanted Tungsten",
				"Enchanted Glacite", "Mithril Powder", "Gemstone Powder", "Bejeweled Collar", "Skeleton Key",
				"Opal Crystal", "Onyx Crystal", "Aquamarine Crystal", "Citrine Crystal", "Peridot Crystal",
				"Dwarven O's Metallic Minis", "Shattered Locket", "Mithril Plate", "Glacite Amalgamation"};
		private static final int CARD_W = 64, GAP = 4, WINNER_INDEX = 34;

		private final String title;
		private final List<Card> cards = new ArrayList<>();
		private final float spin, hold, jitter;
		private int lastPassed = -1;
		private boolean landed;

		Roulette(String corpse, List<Reward> loot, Reward winner, boolean fast) {
			this.title = corpse.toUpperCase(Locale.ROOT) + " CORPSE";
			this.spin = fast ? 1.6f : 4.6f;
			this.hold = fast ? 0.9f : 1.8f;
			Random random = new Random();
			// Land a little off-center, like a real spinner.
			this.jitter = (random.nextFloat() - 0.5f) * CARD_W * 0.6f;
			for (int i = 0; i < WINNER_INDEX + 6; i++) {
				if (i == WINNER_INDEX) {
					cards.add(new Card(winner.item(), winner.value()));
				} else if (!loot.isEmpty() && random.nextInt(3) == 0) {
					Reward r = loot.get(random.nextInt(loot.size()));
					cards.add(new Card(r.item(), r.value()));
				} else {
					String name = FILLER[random.nextInt(FILLER.length)];
					cards.add(new Card(name, Prices.of(name)));
				}
			}
		}

		@Override
		public boolean frame(GuiGraphicsExtractor g, Font font, float t) {
			float total = spin + hold;
			if (t > total) return false;
			float fade = t < 0.2f ? t / 0.2f : t > total - 0.35f ? (total - t) / 0.35f : 1;

			int w = Math.min(360, g.guiWidth() - 20);
			int h = 112;
			int x0 = (g.guiWidth() - w) / 2;
			int y0 = (int) (g.guiHeight() * 0.38f) - h / 2;
			int cx = x0 + w / 2;

			// Panel
			g.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, alpha(Branding.LAMP_GOLD, fade));
			g.fill(x0, y0, x0 + w, y0 + h, alpha(0xF0141010, fade));
			text(g, font, title, cx, y0 + 5, 1.0f, alpha(Branding.LAMP_GOLD, fade));

			// Reel
			float step = CARD_W + GAP;
			float target = WINNER_INDEX * step + jitter;
			float offset = target * easeOutCubic(t / spin);
			int reelTop = y0 + 18, reelBottom = y0 + h - 18;
			g.enableScissor(x0 + 2, reelTop, x0 + w - 2, reelBottom);
			for (int i = 0; i < cards.size(); i++) {
				float left = cx + i * step - offset - CARD_W / 2f;
				if (left > x0 + w || left + CARD_W < x0) continue;
				Card c = cards.get(i);
				int color = rarity(c.value());
				boolean winner = landed && i == WINNER_INDEX;
				int l = Math.round(left);
				g.fill(l, reelTop + 2, l + CARD_W, reelBottom - 2, alpha(winner ? 0xFF2A2018 : 0xFF221B16, fade));
				g.fill(l, reelTop + 2, l + CARD_W, reelTop + 5, alpha(color, fade));
				g.fill(l, reelBottom - 5, l + CARD_W, reelBottom - 2, alpha(color, fade * 0.6f));
				String name = c.name();
				while (font.width(name) * 0.7f > CARD_W - 6 && name.length() > 3) name = name.substring(0, name.length() - 1);
				if (!name.equals(c.name())) name = name.substring(0, name.length() - 1) + ".";
				if (fade > 0.6f) icon(g, c.icon(), l + CARD_W / 2f, reelTop + 21, 24);
				text(g, font, name, l + CARD_W / 2f, reelTop + 38, 0.7f, alpha(Branding.TEXT, fade));
				text(g, font, valueLabel(c.name(), c.value()), l + CARD_W / 2f, reelTop + 50, 0.7f, alpha(color, fade));
			}
			g.disableScissor();

			// Center marker
			g.fill(cx - 1, reelTop - 2, cx + 1, reelBottom + 2, alpha(Branding.LAMP_GOLD, fade));
			g.fill(cx - 4, reelTop - 4, cx + 4, reelTop - 2, alpha(Branding.LAMP_GOLD, fade));
			g.fill(cx - 4, reelBottom + 2, cx + 4, reelBottom + 4, alpha(Branding.LAMP_GOLD, fade));

			// A click every time a card passes the marker, getting higher as it slows down.
			int passed = (int) Math.floor((offset + step / 2) / step);
			if (passed != lastPassed && t < spin) {
				lastPassed = passed;
				play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.0f + 0.6f * clamp01(t / spin), 0.35f);
			}
			if (!landed && t >= spin) {
				landed = true;
				Card win = cards.get(WINNER_INDEX);
				play(win.value() >= 1_000_000 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.8f);
			}
			if (landed) {
				Card win = cards.get(WINNER_INDEX);
				float pulse = 0.5f + 0.5f * (float) Math.sin((t - spin) * 10);
				int color = rarity(win.value());
				text(g, font, win.name() + "  " + valueLabel(win.name(), win.value()), cx, y0 + h - 13, 1.0f,
						alpha(color, fade * (0.7f + 0.3f * pulse)));
			}
			return true;
		}
	}

	// ---- the big drop -----------------------------------------------------------

	private static final class BigDrop implements Scene {
		private record Bit(float x, float y, float vx, float vy, float size, int color) {}

		private static final float LENGTH = 6.5f;
		private static final int[] CONFETTI = {0xFFFFC93C, 0xFFF29BB0, 0xFF4FC9DE, 0xFF7BD88F, 0xFFFFFFFF, 0xFFC77DFF};

		private final String item;
		private final double value;
		private final ItemStack icon;
		private final List<Bit> bits = new ArrayList<>();
		private boolean blast, twinkle1, twinkle2, started;

		BigDrop(String item, double value) {
			this.item = item;
			this.value = value;
			this.icon = SkyBlockItems.icon(item);
			Random r = new Random();
			// Rain from the top, plus two "cannons" firing up from the bottom corners.
			for (int i = 0; i < 70; i++) {
				bits.add(new Bit(r.nextFloat(), -0.05f - r.nextFloat() * 0.3f, (r.nextFloat() - 0.5f) * 0.15f,
						0.15f + r.nextFloat() * 0.25f, 2 + r.nextInt(3), CONFETTI[r.nextInt(CONFETTI.length)]));
			}
			for (int side = 0; side < 2; side++) {
				for (int i = 0; i < 45; i++) {
					float dir = side == 0 ? 1 : -1;
					bits.add(new Bit(side == 0 ? 0.02f : 0.98f, 1.02f, dir * (0.15f + r.nextFloat() * 0.35f),
							-(0.9f + r.nextFloat() * 0.6f), 2 + r.nextInt(3), CONFETTI[r.nextInt(CONFETTI.length)]));
				}
			}
		}

		private String headline() {
			if (value >= 100_000_000) return "INSANE DROP!";
			if (value >= 50_000_000) return "CRAZY DROP!";
			return "RARE DROP!";
		}

		@Override
		public boolean frame(GuiGraphicsExtractor g, Font font, float t) {
			if (t > LENGTH) return false;
			int sw = g.guiWidth(), sh = g.guiHeight();
			float cx = sw / 2f, cy = sh * 0.42f;
			float fade = t > LENGTH - 0.7f ? (LENGTH - t) / 0.7f : 1;

			if (!started) {
				started = true;
				play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
			}
			if (!blast && t > 0.1f) {
				blast = true;
				play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
			}
			if (!twinkle1 && t > 0.9f) {
				twinkle1 = true;
				play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
			}
			if (!twinkle2 && t > 1.5f) {
				twinkle2 = true;
				play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.3f, 1.0f);
			}

			// Darken the game, then a white flash.
			g.fill(0, 0, sw, sh, alpha(0x99000000, clamp01(t / 0.3f) * fade));
			if (t < 0.35f) g.fill(0, 0, sw, sh, alpha(0xD8FFFFFF, 1 - t / 0.35f));

			// Slowly turning light rays behind the text.
			float rayLength = (float) Math.hypot(sw, sh);
			int rayColor = value >= 100_000_000 ? 0xFFFF5577 : Branding.LAMP_GOLD;
			for (int i = 0; i < 18; i++) {
				g.pose().pushMatrix();
				g.pose().translate(cx, cy);
				g.pose().rotate((float) (i * Math.PI * 2 / 18 + t * 0.3f));
				g.fill(0, -3, Math.round(rayLength), 3, alpha(rayColor, 0.16f * clamp01(t / 0.5f) * fade));
				g.pose().popMatrix();
			}

			// The actual drop: big in the middle, popping in, floating, with a pulsing glow behind it.
			float iconIn = easeOutBack((t - 0.15f) / 0.5f);
			if (iconIn > 0.01f) {
				float iy = cy - 92 + (float) Math.sin(t * 2.2f) * 3;
				float pulse = 0.5f + 0.5f * (float) Math.sin(t * 4);
				for (int ring = 4; ring >= 1; ring--) {
					float half = (22 + ring * 9 + pulse * 4) * iconIn;
					g.pose().pushMatrix();
					g.pose().translate(cx, iy);
					g.pose().rotate((float) (Math.PI / 4 + t * 0.4f * (ring % 2 == 0 ? 1 : -1)));
					g.fill(Math.round(-half), Math.round(-half), Math.round(half), Math.round(half),
							alpha(rayColor, (0.10f + 0.04f * (5 - ring)) * fade));
					g.pose().popMatrix();
				}
				if (fade > 0.5f) icon(g, icon, cx, iy, 64 * iconIn);
			}

			// Headline pops in, shaking a little.
			float pop = easeOutBack(t / 0.45f);
			float shake = t < 0.7f ? (1 - t / 0.7f) * 3 : 0;
			float sx = (float) Math.sin(t * 70) * shake, sy = (float) Math.cos(t * 55) * shake;
			if (pop > 0.01f) text(g, font, headline(), cx + sx, cy - 34 + sy, 4.2f * pop, alpha(rayColor, fade));

			// Item name in moving rainbow letters.
			float nameIn = clamp01((t - 0.4f) / 0.4f);
			if (nameIn > 0) {
				float scale = 2.0f;
				float width = font.width(item) * scale;
				float x = cx - width / 2;
				for (int i = 0; i < item.length(); i++) {
					String ch = String.valueOf(item.charAt(i));
					float hue = (t * 0.35f + i * 0.06f) % 1f;
					int rgb = hsb(hue, 0.55f, 1f);
					g.pose().pushMatrix();
					g.pose().translate(x, cy + 14 + (float) Math.sin(t * 6 + i * 0.5f) * 1.5f);
					g.pose().scale(scale, scale);
					g.text(font, ch, 0, 0, alpha(0xFF000000 | rgb, nameIn * fade), true);
					g.pose().popMatrix();
					x += font.width(ch) * scale;
				}
			}

			// Coin counter rolling up.
			float count = easeOutCubic((t - 0.8f) / 1.8f);
			if (t > 0.8f) {
				long shown = Math.round(value * count);
				float bounce = t > 2.6f && t < 2.9f ? 1 + 0.15f * (float) Math.sin((t - 2.6f) / 0.3f * Math.PI) : 1;
				text(g, font, String.format(Locale.ROOT, "%,d coins", shown), cx, cy + 40, 1.6f * bounce,
						alpha(Branding.LAMP_GOLD, fade));
			}

			// Confetti with gravity.
			for (Bit b : bits) {
				float x = (b.x() + b.vx() * t) * sw;
				float y = (b.y() + b.vy() * t + 0.35f * t * t) * sh;
				if (y < -10 || y > sh + 10) continue;
				int s = Math.round(b.size());
				g.fill(Math.round(x), Math.round(y), Math.round(x) + s, Math.round(y) + s, alpha(b.color(), fade));
			}

			// Watermark, handy for clips.
			text(g, font, "SkyMole", cx, sh - 26, 1.0f, alpha(0xCCFFC93C, fade));
			return true;
		}
	}
}
