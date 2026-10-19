package dev.sculike.skymole.hypixel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import dev.sculike.skymole.SkyMole;
import dev.sculike.skymole.config.SkyMoleConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * SkyBlock items: real name -> real ID (from Hypixel's official item list, so "Shattered Locket"
 * is found as SHATTERED_PENDANT), and an icon for each item to draw in SkyMole's scenes.
 *
 * Icons: custom heads use their real skin from the API. Newer items get their look from Hypixel's
 * resource pack in game, which the API doesn't include, so SkyMole learns it from items that pass
 * through your inventory. Until then a close vanilla lookalike is used.
 */
public final class SkyBlockItems {
	private record Info(String id, String material, int durability, String skin, boolean glowing) {}

	private static final String URL = "https://api.hypixel.net/v2/resources/skyblock/items";
	private static volatile Map<String, Info> byName = Map.of();
	private static final Map<String, ItemStack> ICONS = new HashMap<>();

	/** Lookalikes for items whose real look only exists in Hypixel's resource pack. */
	private static final Map<String, String> LOOKALIKE = Map.ofEntries(
			Map.entry("umber", "brown_terracotta"), Map.entry("tungsten", "clay_ball"),
			Map.entry("glacite", "packed_ice"), Map.entry("skeleton key", "trial_key"),
			Map.entry("mithril plate", "iron_ingot"), Map.entry("glacite amalgamation", "snowball"),
			Map.entry("bejeweled collar", "lead"), Map.entry("shattered locket", "heart_of_the_sea"),
			Map.entry("mithril powder", "prismarine_crystals"), Map.entry("gemstone powder", "amethyst_shard"),
			Map.entry("glacite powder", "snowball"), Map.entry("mithril", "prismarine_crystals"));
	private static final Map<String, String> CRYSTAL_GLASS = Map.ofEntries(
			Map.entry("opal", "white"), Map.entry("onyx", "black"), Map.entry("aquamarine", "blue"),
			Map.entry("citrine", "brown"), Map.entry("peridot", "green"), Map.entry("jade", "lime"),
			Map.entry("amber", "orange"), Map.entry("ruby", "red"), Map.entry("sapphire", "light_blue"),
			Map.entry("amethyst", "purple"), Map.entry("topaz", "yellow"), Map.entry("jasper", "magenta"));

	private SkyBlockItems() {}

	/** Downloads the item list in the background (about 5 MB, once per game start). */
	public static void start() {
		CompletableFuture.runAsync(() -> {
			try {
				HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
				HttpRequest request = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(60))
						.header("User-Agent", "SkyMole").GET().build();
				String body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
				Map<String, Info> map = new HashMap<>();
				for (JsonElement e : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("items")) {
					JsonObject o = e.getAsJsonObject();
					if (!o.has("id") || !o.has("name")) continue;
					String skin = o.has("skin") && o.get("skin").isJsonObject() && o.getAsJsonObject("skin").has("value")
							? o.getAsJsonObject("skin").get("value").getAsString() : null;
					Info info = new Info(o.get("id").getAsString(), o.has("material") ? o.get("material").getAsString() : "",
							o.has("durability") ? o.get("durability").getAsInt() : 0, skin,
							o.has("glowing") && o.get("glowing").getAsBoolean());
					map.put(TabList.clean(o.get("name").getAsString()).toLowerCase(Locale.ROOT), info);
				}
				byName = map;
			} catch (Exception ex) {
				SkyMole.LOGGER.warn("Couldn't load the SkyBlock item list: {}", ex.toString());
			}
		});
	}

	/** The SkyBlock ID for an item name, e.g. "Shattered Locket" -> "SHATTERED_PENDANT"; guessed if unknown. */
	public static String idFor(String name) {
		Info info = byName.get(name.toLowerCase(Locale.ROOT));
		return info != null ? info.id() : Prices.toId(name);
	}

	/** Learns the in-game look (resource pack model) of SkyBlock items in your inventory. */
	public static void learn(Player player) {
		Map<String, String> models = SkyMoleConfig.get().fun.learnedIcons;
		boolean changed = false;
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			Identifier model = stack.get(DataComponents.ITEM_MODEL);
			if (data == null || model == null) continue;
			String id = data.copyTag().getStringOr("id", "");
			if (id.isEmpty() || model.toString().equals(models.get(id))) continue;
			models.put(id, model.toString());
			changed = true;
			ICONS.remove(id);
		}
		if (changed) SkyMoleConfig.save();
	}

	/** An icon for an item name, to draw with GuiGraphicsExtractor.item(...). */
	public static ItemStack icon(String name) {
		String id = idFor(name);
		ItemStack cached = ICONS.get(id);
		if (cached != null) return cached;
		ItemStack stack = build(name, id);
		// Only remember it once the item list is in; before that it may be a temporary lookalike.
		if (!byName.isEmpty()) ICONS.put(id, stack);
		return stack;
	}

	private static Item vanilla(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	private static ItemStack build(String name, String id) {
		Info info = byName.get(name.toLowerCase(Locale.ROOT));
		boolean glow = info != null && info.glowing();

		// 1. The real look, learned from your inventory.
		String learned = SkyMoleConfig.get().fun.learnedIcons.get(id);
		if (learned != null) {
			ItemStack s = new ItemStack(Items.PAPER);
			s.set(DataComponents.ITEM_MODEL, Identifier.parse(learned));
			if (glow) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			return s;
		}
		// 2. Custom heads: the real skin from the API.
		if (info != null && info.skin() != null) {
			ItemStack s = new ItemStack(Items.PLAYER_HEAD);
			LinkedHashMultimap<String, Property> props = LinkedHashMultimap.create();
			props.put("textures", new Property("textures", info.skin()));
			UUID uuid = UUID.nameUUIDFromBytes(id.getBytes());
			s.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(uuid, "skymole", new PropertyMap(props))));
			return s;
		}
		// 3. A vanilla base item from the API (old item names, e.g. INK_SACK:4 is lapis).
		if (info != null && !info.material().isEmpty() && !info.material().equals("PAPER")) {
			Item item = info.material().equals("INK_SACK") && info.durability() == 4
					? Items.LAPIS_LAZULI : vanilla(info.material().toLowerCase(Locale.ROOT));
			if (item != Items.AIR) return withGlint(new ItemStack(item), glow);
		}
		// 4. A lookalike.
		String lower = name.toLowerCase(Locale.ROOT);
		boolean enchanted = lower.startsWith("enchanted ");
		String base = enchanted ? lower.substring("enchanted ".length()) : lower;
		String path = LOOKALIKE.get(base);
		if (path == null && base.endsWith(" crystal")) {
			String color = CRYSTAL_GLASS.get(base.substring(0, base.length() - " crystal".length()));
			if (color != null) path = color + "_stained_glass";
		}
		Item item = path != null ? vanilla(path) : Items.PAPER;
		return withGlint(new ItemStack(item == Items.AIR ? Items.PAPER : item), glow || enchanted);
	}

	private static ItemStack withGlint(ItemStack s, boolean glow) {
		if (glow) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return s;
	}
}
