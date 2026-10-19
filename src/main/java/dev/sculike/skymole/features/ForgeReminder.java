package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.config.SkyMoleConfig.ForgeSlot;
import dev.sculike.skymole.hud.HudPanel;
import dev.sculike.skymole.hypixel.ForgeText;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.TabList;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Forge reminder. Reads your forge slots from the tab list's Forge widget and from the Forge
 * menu, saves when each one is done (so it works after a restart and on any island), and tells
 * you once when something is ready.
 */
public final class ForgeReminder {
	/** Times on Hypixel are rounded to minutes; don't treat that rounding as a change. */
	private static final long JITTER_MS = 90_000;

	private static boolean wasInSkyBlock;

	private ForgeReminder() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container)) return;
			String title = TabList.clean(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
			if (!title.contains("forge")) return;
			ScreenEvents.afterTick(screen).register(s -> readMenu(container));
		});
	}

	/** The Forge menu: each forging item says how long it has left (or that it's ready). */
	private static void readMenu(AbstractContainerScreen<?> container) {
		Map<String, ForgeText.Slot> found = new LinkedHashMap<>();
		int number = 0;
		for (Slot slot : container.getMenu().slots) {
			if (slot.container instanceof Inventory) continue; // your own items
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) continue;
			ItemLore lore = stack.get(DataComponents.LORE);
			if (lore == null) continue;
			long left = -1;
			for (Component line : lore.lines()) {
				String text = TabList.clean(line.getString());
				String lower = text.toLowerCase(Locale.ROOT);
				if (lower.contains("remaining")) left = ForgeText.millisLeft(text.substring(text.indexOf(':') + 1));
				else if (lower.contains("ready") || lower.contains("click to claim")) left = 0;
				if (left >= 0) break;
			}
			if (left < 0) continue;
			number++;
			found.put("Slot " + number, new ForgeText.Slot(number, TabList.clean(stack.getHoverName().getString()), left));
		}
		if (!found.isEmpty()) sync(found, true);
	}

	/** Called after every state update. */
	public static void tick() {
		SkyMoleConfig.Forge config = SkyMoleConfig.get().forge;
		readTabWidget();

		long now = System.currentTimeMillis();
		boolean changed = false;
		for (ForgeSlot s : config.slots.values()) {
			if (!s.notified && s.readyAt <= now) {
				s.notified = true;
				changed = true;
				if (config.reminder) Alerts.show("Forge ready!", s.item);
			}
		}
		if (changed) SkyMoleConfig.save();

		// Joining SkyBlock: remind about things that finished while you were away.
		boolean inSkyBlock = SkyBlockState.inSkyBlock();
		if (inSkyBlock && !wasInSkyBlock && config.reminder) {
			List<String> ready = new ArrayList<>();
			for (ForgeSlot s : config.slots.values()) if (s.readyAt <= now) ready.add(s.item);
			if (!ready.isEmpty()) Alerts.chat("Forge: ready to collect: " + String.join(", ", ready));
		}
		wasInSkyBlock = inSkyBlock;
	}

	/** The tab list's Forge widget (e.g. "Forges:"), lines like "1) Refined Mithril: 1h 23m". */
	private static void readTabWidget() {
		for (Map.Entry<String, List<String>> section : SkyBlockState.sections().entrySet()) {
			if (!section.getKey().toLowerCase(Locale.ROOT).contains("forge")) continue;
			Map<String, ForgeText.Slot> found = new LinkedHashMap<>();
			for (String line : section.getValue()) {
				ForgeText.Slot slot = ForgeText.tabLine(line);
				if (slot != null) found.put("Slot " + slot.number(), slot);
			}
			sync(found, true);
			return;
		}
	}

	/**
	 * Updates the saved slots from what Hypixel shows. With {@code complete}, slots that aren't
	 * shown anymore were collected and are removed.
	 */
	private static void sync(Map<String, ForgeText.Slot> found, boolean complete) {
		Map<String, ForgeSlot> saved = SkyMoleConfig.get().forge.slots;
		long now = System.currentTimeMillis();
		boolean changed = false;
		if (complete) changed = saved.keySet().removeIf(k -> !found.containsKey(k));
		for (Map.Entry<String, ForgeText.Slot> e : found.entrySet()) {
			ForgeText.Slot slot = e.getValue();
			long readyAt = now + slot.millisLeft();
			ForgeSlot s = saved.get(e.getKey());
			if (s == null || !s.item.equals(slot.item())) {
				s = new ForgeSlot();
				s.item = slot.item();
				s.readyAt = readyAt;
				// Already ready when first seen: no pop-up for something you can see right now.
				s.notified = slot.millisLeft() == 0;
				saved.put(e.getKey(), s);
				changed = true;
			} else if (Math.abs(s.readyAt - readyAt) > JITTER_MS && slot.millisLeft() > 0) {
				s.readyAt = readyAt;
				s.notified = false;
				changed = true;
			}
		}
		if (changed) SkyMoleConfig.save();
	}

	/** Forge rows for the Tools HUD. */
	public static void addRows(List<HudPanel.Row> rows) {
		SkyMoleConfig.Forge config = SkyMoleConfig.get().forge;
		if (!config.hud) return;
		long now = System.currentTimeMillis();
		for (ForgeSlot s : config.slots.values()) {
			long left = s.readyAt - now;
			if (left <= 0) rows.add(new HudPanel.Row(s.item, "Ready", Branding.GOOD, -1));
			else rows.add(new HudPanel.Row(s.item, format(left), Branding.TEXT_DIM, -1));
		}
	}

	/** For /skymole debug. */
	public static String describe() {
		StringBuilder out = new StringBuilder();
		long now = System.currentTimeMillis();
		SkyMoleConfig.get().forge.slots.forEach((k, s) -> out.append(k).append(": ").append(s.item)
				.append(" ").append(s.readyAt <= now ? "ready" : format(s.readyAt - now)).append("; "));
		return out.isEmpty() ? "none saved" : out.toString();
	}

	static String format(long millis) {
		long s = millis / 1000;
		if (s >= 86_400) return (s / 86_400) + "d " + (s % 86_400 / 3600) + "h";
		if (s >= 3600) return (s / 3600) + "h " + (s % 3600 / 60) + "m";
		return (s / 60) + "m " + (s % 60) + "s";
	}
}
