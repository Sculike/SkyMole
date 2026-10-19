package dev.sculike.skymole;

import java.util.HashSet;
import java.util.Set;

/**
 * Runs a feature so that an error (for example after a Hypixel update) never crashes the game:
 * the error is logged once and that feature is paused until the game restarts.
 */
public final class Safe {
	private static final Set<String> PAUSED = new HashSet<>();

	private Safe() {}

	public static void run(String feature, Runnable action) {
		if (PAUSED.contains(feature)) return;
		try {
			action.run();
		} catch (Throwable t) {
			PAUSED.add(feature);
			SkyMole.LOGGER.error("{} hit an error and was paused until restart", feature, t);
		}
	}

	/** Features paused because of an error, for /skymole debug. */
	public static Set<String> paused() {
		return PAUSED;
	}
}
