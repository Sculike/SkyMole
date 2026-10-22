package dev.sculike.skymole.features;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.SkyBlockItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * A small pop next to the crosshair when Compact procs.
 *
 * <p>Compact fires often, so this is deliberately tiny and silent: the item, its name, a short
 * bounce and it is gone. The big-drop celebration exists for the rare things; something that
 * happens every few minutes has to stay out of the way or it becomes noise you turn off.
 */
public final class CompactPop {
	private static final Pattern COMPACT = Pattern.compile("^COMPACT! You found an? (.+?)!");

	/** Whole life of one pop, in seconds. Short enough not to overlap the next proc. */
	private static final float LIFE = 1.6f;
	/** How long the bounce in takes. */
	private static final float POP_IN = 0.22f;
	/** How long the fade out takes. */
	private static final float FADE = 0.45f;
	/** Pops stack upward; more than this on screen would be clutter. */
	private static final int MAX_SHOWN = 3;

	private record Pop(String item, ItemStack icon, long born) {}

	private static final Deque<Pop> POPS = new ArrayDeque<>();

	private CompactPop() {}

	public static void register() {
		Chat.listen(CompactPop::onChat);
	}

	static void onChat(String text) {
		if (!SkyMoleConfig.get().fun.compactPop) return;
		Matcher m = COMPACT.matcher(text);
		if (!m.find()) return;
		add(m.group(1).trim());
	}

	/** Shown for /skymole test compact as well as the real thing. */
	public static void add(String item) {
		POPS.addLast(new Pop(item, SkyBlockItems.icon(item), System.nanoTime()));
		while (POPS.size() > MAX_SHOWN) POPS.removeFirst();
	}

	/** Overshoots a little on the way in, which is what makes it feel like a pop. */
	private static float easeOutBack(float t) {
		if (t <= 0) return 0;
		if (t >= 1) return 1;
		float a = 2.2f;
		float u = t - 1;
		return 1 + u * u * ((a + 1) * u + a);
	}

	public static void render(GuiGraphicsExtractor g) {
		if (POPS.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden()) return;

		POPS.removeIf(p -> seconds(p) > LIFE);
		if (POPS.isEmpty()) return;

		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2;
		int slot = 0;
		for (Pop p : POPS) {
			drawOne(g, mc, p, cx, cy, slot++);
		}
	}

	private static float seconds(Pop p) {
		return (System.nanoTime() - p.born()) / 1_000_000_000f;
	}

	private static void drawOne(GuiGraphicsExtractor g, Minecraft mc, Pop p, int cx, int cy, int slot) {
		float t = seconds(p);
		float in = easeOutBack(t / POP_IN);
		float alpha = t > LIFE - FADE ? (LIFE - t) / FADE : 1;
		if (alpha <= 0.02f) return;

		// Sits to the right of the crosshair and drifts up as it goes, out of the way of what
        // you are aiming at. Older pops sit higher, so a run of procs reads as a little stack.
		float rise = 10 * (t / LIFE);
		int x = cx + 18;
		int y = cy - 6 - slot * 20 - Math.round(rise);

		String label = p.item();
		int textWidth = mc.font.width(label);
		int width = 20 + textWidth + 6;

		int a = Math.round(alpha * 255);
		g.fill(x, y, x + width, y + 18, (Math.round(alpha * 0.55f * 255) << 24));
		g.fill(x, y, x + 1, y + 18, (a << 24) | (Branding.LAMP_GOLD & 0xFFFFFF));

		// The item itself, bouncing in.
		g.pose().pushMatrix();
		g.pose().translate(x + 3, y + 1);
		g.pose().scale(in, in);
		g.item(p.icon(), 0, 0);
		g.pose().popMatrix();

		g.text(mc.font, label, x + 21, y + 5, (a << 24) | (Branding.TEXT & 0xFFFFFF), true);
	}

	/** For /skymole debug. */
	public static String describe() {
		return "showing=" + POPS.size() + " enabled=" + SkyMoleConfig.get().fun.compactPop;
	}
}
