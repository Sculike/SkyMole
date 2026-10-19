package dev.sculike.skymole.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.sculike.skymole.SkyMole;
import net.fabricmc.loader.api.FabricLoader;

/**
 * All SkyMole settings, saved as config/skymole.json.
 * New fields get their default value automatically when an older file is loaded.
 */
public final class SkyMoleConfig {
	/**
	 * Position, size and on/off of one HUD panel. With {@code right}, x is the distance
	 * from the right edge of the screen, so the panel stays put on any screen size.
	 */
	public static final class Hud {
		public boolean enabled = true;
		public int x;
		public int y;
		public boolean right;
		public int scalePercent = 100;

		public Hud() {}

		Hud(int x, int y, boolean right) {
			this.x = x;
			this.y = y;
			this.right = right;
		}

		public void moveTo(Hud other) {
			x = other.x;
			y = other.y;
			right = other.right;
		}
	}

	/** Bumped when saved settings need a one-time fix on load. Old files have none, so 0. */
	public int version;

	public static final class Commissions {
		public Hud hud = new Hud(4, 110, false);
		/** Index into {@link #DISPLAY_MODES}. */
		public int display = 0;
		public boolean newCommissionMessage = true;
		public boolean doneAlert = true;
		public boolean claimReminder = true;
		/** "Press T, right-click for a waypoint" under the commissions. */
		public boolean waypointHint = true;

		public static final String[] DISPLAY_MODES = {"Bar + %", "Bar + count", "Bar only", "% only", "Count only", "Nothing"};

		public boolean showBar() { return display <= 2; }
		public boolean showPercent() { return display == 0 || display == 3; }
		public boolean showCount() { return display == 1 || display == 4; }
	}

	public static final class Powder {
		public Hud hud = new Hud(4, 175, false);
		public boolean mithril = true;
		public boolean gemstone = true;
		public boolean glacite = true;
		public boolean rates = true;
		/** Stop the clock after this many minutes without gaining powder (0 = never). */
		public int pauseAfterMinutes = 2;
		public boolean pristine = true;
		public boolean chests = true;
	}

	public static final class Events {
		public Hud hud = new Hud(4, 4, true);
		public boolean startAlert = true;
		public boolean endAlert = false;
		public boolean goblinAlert = true;
		public boolean powderGhastAlert = true;
	}

	public static final class Tools {
		public Hud hud = new Hud(4, 265, false);
		public boolean ability = true;
		public boolean abilityReadyAlert = true;
		public boolean fuel = true;
		/** Warn when drill fuel drops below this percent (0 = never). */
		public int lowFuelPercent = 10;
		public boolean stats = true;
		public boolean pickobulusPreview = true;
		/** Pickobulus blast: radius in blocks and whether only ores break. Calibrated from your throws. */
		public double pickobulusRadius = 2.0;
		public boolean pickobulusOresOnly = false;
		public boolean titaniumAlert = true;
		public boolean titaniumOutline = true;
		/** Outline the best next block for Mining Spread. Off by default: you choose to use it. */
		public boolean bestBlock = false;
		public boolean bestBlockSecond = true;
		/** Last Mining/Gemstone Spread seen in a Hypixel menu (-1 = never seen). */
		public double miningSpreadSeen = -1;
		public double gemstoneSpreadSeen = -1;
		/** Your own value, used only when SkyMole hasn't seen it anywhere (0 = not set). */
		public int manualSpread = 0;
	}

	public static final class Area {
		public Hud hud = new Hud(4, 50, true);
		public boolean zone = true;
		public boolean cold = true;
		/** Warn at this much cold (Glacite Tunnels; 100 sends you back). 0 = never. */
		public int coldWarning = 75;
		public boolean crystals = true;
		public boolean lobbyDay = true;
		public boolean skyMall = true;
		public boolean wormAlert = true;
		public boolean wormCooldown = true;
		public boolean mineshaftAlert = true;
		/** Outline mining mobs (only where you can see them, never through walls). */
		public boolean mobOutlines = true;
		/** Outline the Forge NPCs while you're at the Forge. */
		public boolean forgeOutline = true;
	}

	public static final class Solvers {
		public boolean fetchur = true;
		public boolean puzzler = true;
	}

	/** Optional animations: corpse "case opening" and big-drop celebrations. */
	public static final class Fun {
		public boolean corpseRoulette = true;
		/** 0 = normal, 1 = fast */
		public int rouletteSpeed = 0;
		public boolean bigDrop = true;
		/** Celebrate drops worth at least this many million coins. */
		public int bigDropMillions = 10;
		/** Learned in-game item looks: SkyBlock ID -> resource pack item model. */
		public java.util.Map<String, String> learnedIcons = new java.util.HashMap<>();
	}

	public static final class Forge {
		public boolean reminder = true;
		public boolean hud = true;
		/** Saved forge slots ("Slot 1" -> item and finish time), so reminders survive restarts. */
		public java.util.Map<String, ForgeSlot> slots = new java.util.LinkedHashMap<>();
	}

	public static final class ForgeSlot {
		public String item = "";
		/** When it's done, in milliseconds since 1970 (System.currentTimeMillis). */
		public long readyAt;
		/** True once the "ready" alert was shown. */
		public boolean notified;
	}

	public static final class Markers {
		public Hud hud = new Hud(4, 80, false);
		public boolean enabled = true;
		public boolean beam = true;
		/** A waypoint for every active commission, without clicking. */
		public boolean autoCommissions = true;
		/** Right-clicked waypoint gets a line along the fastest walking route (Dwarven Mines). */
		public boolean routeLine = true;
		/** Commissions whose waypoint you've reached (lower case), hidden until the commission is done. */
		public java.util.Set<String> reached = new java.util.HashSet<>();
		/** Your own spots from /skymole marker set, by name: {x, y, z}. They win over the built-in ones. */
		public java.util.Map<String, int[]> custom = new java.util.HashMap<>();
	}

	public static final class General {
		public int backgroundOpacityPercent = 80;
		public boolean textShadow = true;
		/** 0 = light tint, 1 = the game's blur, 2 = nothing */
		public int menuBackground = 0;
		/** Index into AlertSounds.NAMES, used by every SkyMole alert. */
		public int alertSound = 0;
		public int volumePercent = 80;
	}

	public Commissions commissions = new Commissions();
	public Powder powder = new Powder();
	public Events events = new Events();
	public Tools tools = new Tools();
	public Area area = new Area();
	public Solvers solvers = new Solvers();
	public Markers markers = new Markers();
	public Forge forge = new Forge();
	public Fun fun = new Fun();
	public General general = new General();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skymole.json");
	private static final int CURRENT_VERSION = 3;
	private static SkyMoleConfig instance = new SkyMoleConfig();

	public static SkyMoleConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			instance.version = CURRENT_VERSION;
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			SkyMoleConfig loaded = GSON.fromJson(reader, SkyMoleConfig.class);
			if (loaded != null) instance = loaded;
			int from = instance.version;
			if (from < 2) {
				// Before version 2 panels could only sit on the left and overlapped: use the new layout once.
				SkyMoleConfig d = new SkyMoleConfig();
				instance.commissions.hud.moveTo(d.commissions.hud);
				instance.powder.hud.moveTo(d.powder.hud);
				instance.events.hud.moveTo(d.events.hud);
				instance.tools.hud.moveTo(d.tools.hud);
				instance.area.hud.moveTo(d.area.hud);
			}
			if (from < 3) {
				// The orange "next block" used to be an easy-to-miss extra; it's part of the route now.
				instance.tools.bestBlockSecond = true;
			}
			if (from < CURRENT_VERSION) {
				instance.version = CURRENT_VERSION;
				save();
			}
		} catch (Exception e) {
			// A broken file shouldn't crash the game: keep defaults and say why.
			SkyMole.LOGGER.error("Couldn't read {}, using defaults", FILE, e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			SkyMole.LOGGER.error("Couldn't save {}", FILE, e);
		}
	}
}
