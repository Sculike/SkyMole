package dev.sculike.skymole.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import dev.sculike.skymole.Branding;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One line in the SkyMole menu: a section header, a description, a setting or a button. */
public abstract class MenuEntry {
	static final int ROW_HEIGHT = 16;
	private static final int ROW_BG = 0xFF261D16;
	private static final int ROW_BG_HOVER = 0xFF2C221A;
	private static final int ROW_BORDER = 0xFF3A2C20;
	private static final int ROW_BORDER_HOVER = 0xFF5A4430;
	private static final int TRACK = 0xFF3A2E24;

	public abstract int height();

	public abstract void draw(GuiGraphicsExtractor g, Font font, int x, int y, int width, boolean hover);

	/** Left or right click inside this entry. Returns true if something changed. */
	public boolean click(double mouseX, int button, int x, int width) {
		return false;
	}

	/** Mouse dragged while this entry was clicked (only sliders care). */
	public void drag(double mouseX, int x, int width) {}

	// ---- Text entries -------------------------------------------------------

	public static final class Header extends MenuEntry {
		private final String title;

		public Header(String title) {
			this.title = title.toUpperCase(java.util.Locale.ROOT);
		}

		@Override public int height() { return 16; }

		@Override
		public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int width, boolean hover) {
			g.text(font, title, x, y + 6, Branding.LAMP_GOLD, false);
		}
	}

	public static final class Description extends MenuEntry {
		private final String text;

		public Description(String text) {
			this.text = text;
		}

		@Override public int height() { return 12; }

		@Override
		public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int width, boolean hover) {
			g.text(font, text, x, y + 1, Branding.TEXT_DIM, false);
		}
	}

	// ---- Settings ------------------------------------------------------------

	/** A setting row: label on the left, control on the right, right-click resets it. */
	public abstract static class Setting extends MenuEntry {
		protected final String label;

		Setting(String label) {
			this.label = label;
		}

		@Override public int height() { return ROW_HEIGHT + 2; }

		public abstract boolean isDefault();

		public abstract void reset();

		protected abstract void drawControl(GuiGraphicsExtractor g, Font font, int right, int y);

		protected int labelColor() {
			return Branding.TEXT;
		}

		@Override
		public final void draw(GuiGraphicsExtractor g, Font font, int x, int y, int width, boolean hover) {
			g.fill(x, y, x + width, y + ROW_HEIGHT, hover ? ROW_BORDER_HOVER : ROW_BORDER);
			g.fill(x + 1, y + 1, x + width - 1, y + ROW_HEIGHT - 1, hover ? ROW_BG_HOVER : ROW_BG);
			g.text(font, label, x + 6, y + 4, labelColor(), false);
			if (!isDefault()) {
				// Small gold dot after the label: "you changed this".
				int dotX = x + 6 + font.width(label) + 4;
				g.fill(dotX, y + 7, dotX + 2, y + 9, Branding.LAMP_GOLD);
			}
			drawControl(g, font, x + width - 6, y);
		}

		@Override
		public boolean click(double mouseX, int button, int x, int width) {
			if (button == 1) {
				if (isDefault()) return false;
				reset();
				return true;
			}
			return button == 0 && leftClick(mouseX, x, width);
		}

		protected abstract boolean leftClick(double mouseX, int x, int width);
	}

	public static class Toggle extends Setting {
		private final BooleanSupplier get;
		private final Consumer<Boolean> set;
		private final boolean def;

		public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set, boolean def) {
			super(label);
			this.get = get;
			this.set = set;
			this.def = def;
		}

		@Override public boolean isDefault() { return get.getAsBoolean() == def; }
		@Override public void reset() { set.accept(def); }

		@Override
		protected void drawControl(GuiGraphicsExtractor g, Font font, int right, int y) {
			drawSwitch(g, right, y, get.getAsBoolean());
		}

		static void drawSwitch(GuiGraphicsExtractor g, int right, int y, boolean on) {
			int left = right - 20;
			int top = y + 4;
			g.fill(left, top, right, top + 8, on ? Branding.LAMP_GOLD : TRACK);
			int knobX = on ? right - 7 : left + 1;
			g.fill(knobX, top + 1, knobX + 6, top + 7, on ? Branding.TUNNEL : Branding.TEXT_DIM);
		}

		@Override
		protected boolean leftClick(double mouseX, int x, int width) {
			set.accept(!get.getAsBoolean());
			return true;
		}
	}

	public static final class Slider extends Setting {
		private static final int BAR_WIDTH = 90;
		private static final int VALUE_WIDTH = 30;
		private final IntSupplier get;
		private final IntConsumer set;
		private final int min, max, step, def;
		private final String unit;

		public Slider(String label, IntSupplier get, IntConsumer set, int min, int max, int step, int def, String unit) {
			super(label);
			this.get = get;
			this.set = set;
			this.min = min;
			this.max = max;
			this.step = step;
			this.def = def;
			this.unit = unit;
		}

		@Override public boolean isDefault() { return get.getAsInt() == def; }
		@Override public void reset() { set.accept(def); }

		private int barRight(int x, int width) { return x + width - 6 - VALUE_WIDTH; }

		@Override
		protected void drawControl(GuiGraphicsExtractor g, Font font, int right, int y) {
			int barRight = right - VALUE_WIDTH;
			int barLeft = barRight - BAR_WIDTH;
			int mid = y + ROW_HEIGHT / 2;
			float f = (get.getAsInt() - min) / (float) (max - min);
			int knob = barLeft + Math.round(f * BAR_WIDTH);
			g.fill(barLeft, mid - 1, barRight, mid + 1, TRACK);
			g.fill(barLeft, mid - 1, knob, mid + 1, Branding.LAMP_GOLD);
			g.fill(knob - 1, mid - 4, knob + 1, mid + 4, Branding.TEXT);
			String value = get.getAsInt() + unit;
			g.text(font, value, right - font.width(value), y + 4, Branding.TEXT_DIM, false);
		}

		@Override
		protected boolean leftClick(double mouseX, int x, int width) {
			int barRight = barRight(x, width);
			if (mouseX < barRight - BAR_WIDTH - 4 || mouseX > barRight + 4) return false;
			drag(mouseX, x, width);
			return true;
		}

		@Override
		public void drag(double mouseX, int x, int width) {
			int barRight = barRight(x, width);
			float f = (float) ((mouseX - (barRight - BAR_WIDTH)) / BAR_WIDTH);
			f = Math.max(0, Math.min(1, f));
			int value = min + Math.round(f * (max - min) / step) * step;
			set.accept(Math.max(min, Math.min(max, value)));
		}

		public boolean grabs(double mouseX, int x, int width) {
			int barRight = barRight(x, width);
			return mouseX >= barRight - BAR_WIDTH - 4 && mouseX <= barRight + 4;
		}
	}

	public static final class Cycle extends Setting {
		private final String[] options;
		private final IntSupplier get;
		private final IntConsumer set;
		private final int def;
		private IntConsumer onChange = v -> { };

		public Cycle(String label, String[] options, IntSupplier get, IntConsumer set, int def) {
			super(label);
			this.options = options;
			this.get = get;
			this.set = set;
			this.def = def;
		}

		/** Runs after the value changes, e.g. to play the newly picked sound. */
		public Cycle onChange(IntConsumer onChange) {
			this.onChange = onChange;
			return this;
		}

		@Override public boolean isDefault() { return get.getAsInt() == def; }

		@Override
		public void reset() {
			set.accept(def);
			onChange.accept(def);
		}

		private int index() {
			return Math.floorMod(get.getAsInt(), options.length);
		}

		private int controlWidth(Font font) {
			int widest = 0;
			for (String option : options) widest = Math.max(widest, font.width(option));
			return widest + 24;
		}

		@Override
		protected void drawControl(GuiGraphicsExtractor g, Font font, int right, int y) {
			String value = options[index()];
			int left = right - controlWidth(font);
			g.text(font, "<", left, y + 4, Branding.LAMP_GOLD, false);
			g.text(font, ">", right - font.width(">"), y + 4, Branding.LAMP_GOLD, false);
			int center = (left + right) / 2;
			g.text(font, value, center - font.width(value) / 2, y + 4, Branding.TEXT, false);
		}

		@Override
		protected boolean leftClick(double mouseX, int x, int width) {
			// Clicking the left half of the control goes back, anywhere else goes forward.
			int right = x + width - 6;
			int left = right - controlWidth(net.minecraft.client.Minecraft.getInstance().font);
			int dir = mouseX >= left && mouseX < (left + right) / 2.0 ? -1 : 1;
			int value = Math.floorMod(index() + dir, options.length);
			set.accept(value);
			onChange.accept(value);
			return true;
		}
	}

	// ---- Button --------------------------------------------------------------

	public static final class Button extends MenuEntry {
		private final String label;
		private final Runnable action;

		public Button(String label, Runnable action) {
			this.label = label;
			this.action = action;
		}

		@Override public int height() { return ROW_HEIGHT + 6; }

		@Override
		public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int width, boolean hover) {
			int top = y + 4;
			g.fill(x, top, x + width, top + ROW_HEIGHT, Branding.LAMP_GOLD);
			if (!hover) g.fill(x + 1, top + 1, x + width - 1, top + ROW_HEIGHT - 1, ROW_BG);
			g.text(font, label, x + (width - font.width(label)) / 2, top + 4,
					hover ? Branding.TUNNEL : Branding.LAMP_GOLD, false);
		}

		@Override
		public boolean click(double mouseX, int button, int x, int width) {
			if (button != 0) return false;
			action.run();
			return true;
		}
	}
}
