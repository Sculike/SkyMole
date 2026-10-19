package dev.sculike.skymole.ui;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One category in the menu sidebar. */
public final class MenuTab {
	public final String name;
	public final List<MenuEntry> entries;
	private final Item item;
	private ItemStack icon;

	public MenuTab(String name, Item item, List<MenuEntry> entries) {
		this.name = name;
		this.item = item;
		this.entries = entries;
	}

	/**
	 * The sidebar icon. Item data only exists once you're in a world, so on the
	 * title screen (e.g. opened from Mod Menu) this is null and the icon is left out.
	 */
	public ItemStack icon() {
		if (icon == null && item.builtInRegistryHolder().areComponentsBound()) icon = new ItemStack(item);
		return icon;
	}
}
