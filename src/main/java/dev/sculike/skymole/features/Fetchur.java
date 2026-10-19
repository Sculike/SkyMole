package dev.sculike.skymole.features;

import java.util.Locale;
import java.util.Map;

import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.hypixel.Chat;

/** Fetchur asks for a different item each day as a riddle; this puts the answer in chat. */
public final class Fetchur {
	/** Riddle (part of it) -> what Fetchur wants. */
	private static final Map<String, String> ANSWERS = Map.ofEntries(
			Map.entry("yellow and see through", "20x Yellow Stained Glass"),
			Map.entry("circular and sometimes moves", "1x Compass"),
			Map.entry("expensive minerals", "20x Mithril"),
			Map.entry("useful during celebrations", "1x Firework Rocket"),
			Map.entry("hot and gives energy", "1x Cheap Coffee"),
			Map.entry("tall and can be opened", "1x Wooden Door"),
			Map.entry("brown and fluffy", "3x Rabbit's Foot"),
			Map.entry("explosive but more than usual", "1x Superboom TNT"),
			Map.entry("wearable and grows", "1x Pumpkin"),
			Map.entry("shiny and makes sparks", "1x Flint and Steel"),
			Map.entry("some dudes trade stuff for it", "50x Emerald"),
			Map.entry("red and soft", "50x Red Wool"));

	private Fetchur() {}

	public static void register() {
		Chat.listen(text -> {
			if (!SkyMoleConfig.get().solvers.fetchur || !text.startsWith("[NPC] Fetchur:")) return;
			String answer = answer(text);
			if (answer != null) Alerts.chat("Fetchur wants: " + answer);
		});
	}

	static String answer(String message) {
		String lower = message.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> e : ANSWERS.entrySet()) {
			if (lower.contains(e.getKey())) return e.getValue();
		}
		return null;
	}
}
