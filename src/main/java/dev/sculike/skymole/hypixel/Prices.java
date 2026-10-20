package dev.sculike.skymole.hypixel;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.sculike.skymole.SkyMole;

/**
 * Item prices, fetched in the background (never blocks the game):
 * <ol>
 *   <li>Hypixel's official bazaar API, refreshed every 5 minutes</li>
 *   <li>a public lowest-BIN list for auction items, every 30 minutes</li>
 *   <li>per-item auction prices as a fallback when that list is unavailable</li>
 * </ol>
 */
public final class Prices {
	private static final String BAZAAR = "https://api.hypixel.net/v2/skyblock/bazaar";
	private static final String LOWEST_BIN = "https://moulberry.codes/lowestbin.json";
	private static final String ITEM_PRICE = "https://sky.coflnet.com/api/item/price/%s/current";

	/** Auction items that can drop from corpses: their prices are fetched up front. */
	private static final List<String> CORPSE_ITEMS = List.of(
			"Skeleton Key", "Bejeweled Collar", "Shattered Locket", "Dwarven O's Metallic Minis",
			"Opal Crystal", "Onyx Crystal", "Aquamarine Crystal", "Citrine Crystal", "Peridot Crystal",
			"Jade Crystal", "Amber Crystal", "Topaz Crystal", "Sapphire Crystal", "Ruby Crystal",
			"Amethyst Crystal", "Jasper Crystal", "Glacite Amalgamation", "Mithril Plate", "Frozen Bark");

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static volatile Map<String, Double> bazaar = Map.of();
	private static volatile Map<String, Double> lowestBin = Map.of();
	/** Per-item auction prices, used when the bulk list is unavailable. */
	private static final Map<String, Double> perItem = new ConcurrentHashMap<>();
	private static final Set<String> pending = ConcurrentHashMap.newKeySet();
	private static ScheduledExecutorService pool;

	private Prices() {}

	public static void start() {
		pool = Executors.newScheduledThreadPool(2, r -> {
			Thread t = new Thread(r, "SkyMole prices");
			t.setDaemon(true);
			return t;
		});
		// Bazaar every 5 minutes (about as live as Hypixel's API gets), auctions every 30 (big list).
		pool.scheduleAtFixedRate(Prices::refreshBazaar, 0, 5, TimeUnit.MINUTES);
		pool.scheduleAtFixedRate(Prices::refreshLowestBin, 0, 30, TimeUnit.MINUTES);
		// Prices for the rare corpse drops, so a big drop is recognised the moment it happens.
		pool.scheduleAtFixedRate(() -> CORPSE_ITEMS.forEach(Prices::prefetch), 20, 30 * 60, TimeUnit.SECONDS);
	}

	private static String get(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(25)).header("User-Agent", "SkyMole").GET().build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) throw new IllegalStateException(url + " returned " + response.statusCode());
		return response.body();
	}

	private static void refreshBazaar() {
		try {
			Map<String, Double> map = new HashMap<>();
			JsonObject products = JsonParser.parseString(get(BAZAAR)).getAsJsonObject().getAsJsonObject("products");
			for (Map.Entry<String, JsonElement> e : products.entrySet()) {
				JsonObject quick = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
				if (quick != null) map.put(e.getKey(), quick.get("sellPrice").getAsDouble());
			}
			bazaar = map;
		} catch (Exception e) {
			SkyMole.LOGGER.warn("Couldn't load bazaar prices: {}", e.toString());
		}
	}

	private static void refreshLowestBin() {
		try {
			Map<String, Double> map = new HashMap<>();
			for (Map.Entry<String, JsonElement> e : JsonParser.parseString(get(LOWEST_BIN)).getAsJsonObject().entrySet()) {
				map.put(e.getKey(), e.getValue().getAsDouble());
			}
			lowestBin = map;
		} catch (Exception e) {
			// This list is often unavailable; per-item lookups cover it.
			SkyMole.LOGGER.warn("Couldn't load lowest BIN prices, using per-item lookups: {}", e.toString());
		}
	}

	/** Looks up one auction item's price in the background, if we don't have it yet. */
	public static void prefetch(String itemName) {
		if (pool == null) return;
		String id = SkyBlockItems.idFor(itemName);
		if (bazaar.containsKey(id) || lowestBin.containsKey(id) || perItem.containsKey(id) || !pending.add(id)) return;
		pool.execute(() -> {
			try {
				String body = get(String.format(ITEM_PRICE, URLEncoder.encode(id, StandardCharsets.UTF_8)));
				JsonObject o = JsonParser.parseString(body).getAsJsonObject();
				if (o.has("sell") && !o.get("sell").isJsonNull()) {
					double sell = o.get("sell").getAsDouble();
					if (sell > 0) perItem.put(id, sell);
				}
			} catch (Exception e) {
				SkyMole.LOGGER.debug("No price for {}: {}", id, e.toString());
			} finally {
				pending.remove(id);
			}
		});
	}

	/** "Shattered Locket" -> "SHATTERED_LOCKET": a fallback when the item isn't in Hypixel's list. */
	static String toId(String name) {
		return name.toUpperCase(Locale.ROOT).replace("'", "").replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
	}

	/** The best known price of one of this item (by its display name), or -1 if unknown. */
	public static double of(String itemName) {
		String id = SkyBlockItems.idFor(itemName); // real ID: "Shattered Locket" is SHATTERED_PENDANT
		double best = -1;
		Double bin = lowestBin.get(id);
		if (bin != null) best = bin;
		Double bz = bazaar.get(id);
		if (bz != null) best = Math.max(best, bz);
		Double item = perItem.get(id);
		if (item != null) best = Math.max(best, item);
		if (best < 0) prefetch(itemName); // unknown: look it up for next time
		return best;
	}

	/** True once at least one price source has loaded. */
	public static boolean loaded() {
		return !bazaar.isEmpty() || !lowestBin.isEmpty() || !perItem.isEmpty();
	}

	/** For /skymole debug. */
	public static String describe() {
		return "bazaar=" + bazaar.size() + " lowestBin=" + lowestBin.size() + " perItem=" + perItem.size();
	}
}
