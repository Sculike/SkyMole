package dev.sculike.skymole.hypixel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.sculike.skymole.SkyMole;

/**
 * Item prices, refreshed every 30 minutes in the background (never blocks the game):
 * Hypixel's official bazaar API, and a public lowest-BIN list for auction items.
 */
public final class Prices {
	private static final String BAZAAR = "https://api.hypixel.net/v2/skyblock/bazaar";
	private static final String LOWEST_BIN = "https://moulberry.codes/lowestbin.json";

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static volatile Map<String, Double> bazaar = Map.of();
	private static volatile Map<String, Double> lowestBin = Map.of();

	private Prices() {}

	public static void start() {
		ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "SkyMole prices");
			t.setDaemon(true);
			return t;
		});
		// Bazaar every 5 minutes (about as live as Hypixel's API gets), auctions every 30 (big list).
		pool.scheduleAtFixedRate(Prices::refreshBazaar, 0, 5, TimeUnit.MINUTES);
		pool.scheduleAtFixedRate(Prices::refreshLowestBin, 0, 30, TimeUnit.MINUTES);
	}

	private static JsonObject get(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(20)).header("User-Agent", "SkyMole").GET().build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) throw new IllegalStateException(url + " returned " + response.statusCode());
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private static void refreshBazaar() {
		try {
			Map<String, Double> map = new HashMap<>();
			JsonObject products = get(BAZAAR).getAsJsonObject("products");
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
			for (Map.Entry<String, JsonElement> e : get(LOWEST_BIN).entrySet()) map.put(e.getKey(), e.getValue().getAsDouble());
			lowestBin = map;
		} catch (Exception e) {
			SkyMole.LOGGER.warn("Couldn't load lowest BIN prices: {}", e.toString());
		}
	}

	/** "Shattered Locket" -> "SHATTERED_LOCKET", "Dwarven O's Metallic Minis" -> "DWARVEN_OS_METALLIC_MINIS". */
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
		return best;
	}

	/** True once at least one price list has loaded. */
	public static boolean loaded() {
		return !bazaar.isEmpty() || !lowestBin.isEmpty();
	}
}
