package dev.sculike.skymole.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.SkyBlockState.Commission;
import dev.sculike.skymole.hypixel.TabList;
import dev.sculike.skymole.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * In Hypixel's Commissions menu, right-clicking a commission sets a SkyMole waypoint
 * instead of being sent to Hypixel. Left-click still works as normal.
 */
public final class CommissionMenu {
	private CommissionMenu() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
			if (!isCommissionMenu(screen)) return;
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				if (event.button() != 1 || !SkyMoleConfig.get().markers.enabled) return true;
				Slot slot = ((AbstractContainerScreenAccessor) s).skymole$hoveredSlot();
				if (slot == null || !slot.hasItem()) return true;
				Commission c = commissionOn(slot.getItem());
				if (c == null) return true;
				Markers.forCommission(c.name(), c.done());
				s.onClose(); // close the menu so you can walk to the marker
				return false; // handled by SkyMole, not sent to Hypixel
			});
		});

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!SkyMoleConfig.get().markers.enabled || !isCommissionMenu(Minecraft.getInstance().gui.screen())) return;
			Commission c = commissionOn(stack);
			if (c == null) return;
			lines.add(Component.literal(c.done() ? "Right-click: SkyMole waypoint to an Emissary" : "Right-click: SkyMole waypoint")
					.withColor(Branding.LAMP_GOLD & 0xFFFFFF));
		});
	}

	static boolean isCommissionMenu(Screen screen) {
		return screen instanceof AbstractContainerScreen<?>
				&& TabList.clean(screen.getTitle().getString()).toLowerCase(Locale.ROOT).contains("commission");
	}

	/**
	 * Which of your current commissions (from the tab list) this menu item is about:
	 * its name or lore mentions the commission's name.
	 */
	static Commission commissionOn(ItemStack stack) {
		if (stack.isEmpty()) return null;
		List<String> texts = new ArrayList<>();
		texts.add(TabList.clean(stack.getHoverName().getString()));
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) for (Component line : lore.lines()) texts.add(TabList.clean(line.getString()));

		Commission best = null;
		for (Commission c : SkyBlockState.commissions()) {
			String name = c.name().toLowerCase(Locale.ROOT);
			for (String text : texts) {
				if (!text.toLowerCase(Locale.ROOT).contains(name)) continue;
				// Prefer the longest match ("Goblin Raid Slayer" over "Goblin Raid").
				if (best == null || c.name().length() > best.name().length()) best = c;
			}
		}
		return best;
	}
}
