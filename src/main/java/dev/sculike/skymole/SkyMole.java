package dev.sculike.skymole;

import java.util.function.Supplier;

import dev.sculike.skymole.command.SkyMoleCommand;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.dev.DevShowcase;
import dev.sculike.skymole.dev.DevShowcaseWorld;
import dev.sculike.skymole.features.BestBlock;
import dev.sculike.skymole.features.ChatClicks;
import dev.sculike.skymole.features.CorpseLoot;
import dev.sculike.skymole.features.PickobulusPreview;
import dev.sculike.skymole.features.PuzzlerSolver;
import dev.sculike.skymole.features.TitaniumAlert;
import dev.sculike.skymole.features.Welcome;
import dev.sculike.skymole.ui.Keybinds;
import dev.sculike.skymole.hypixel.Prices;
import dev.sculike.skymole.hypixel.SkyBlockItems;
import dev.sculike.skymole.ui.Cutscenes;
import dev.sculike.skymole.features.CommissionMenu;
import dev.sculike.skymole.features.Commissions;
import dev.sculike.skymole.features.Markers;
import dev.sculike.skymole.features.MobOutlines;
import dev.sculike.skymole.hypixel.Spots;
import dev.sculike.skymole.hypixel.SpreadStats;
import dev.sculike.skymole.features.Events;
import dev.sculike.skymole.features.Fetchur;
import dev.sculike.skymole.features.ForgeReminder;
import dev.sculike.skymole.features.MiningInfo;
import dev.sculike.skymole.features.PowderTracker;
import dev.sculike.skymole.features.Tools;
import dev.sculike.skymole.hud.HudManager;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.SkyBlockState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyMole implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Branding.NAME);

	/** Re-read the tab list, sidebar and boss bar every 10 ticks (twice a second). */
	private static final int UPDATE_INTERVAL = 10;
	private static int ticks;
	private static Supplier<Screen> openNextTick;

	/** Commands run while chat is still open, so screens open on the next tick instead. */
	public static void openNextTick(Supplier<Screen> screen) {
		openNextTick = screen;
	}

	@Override
	public void onInitializeClient() {
		SkyMoleConfig.load();

		Spots.load();
		SpreadStats.register();
		Chat.register();
		ChatClicks.register();
		CommissionMenu.register();
		Commissions.register();
		ForgeReminder.register();
		PowderTracker.register();
		Events.register();
		Tools.register();
		MiningInfo.register();
		Fetchur.register();
		PuzzlerSolver.register();
		PickobulusPreview.register();
		CorpseLoot.register();
		Prices.start();
		SkyBlockItems.start();
		Keybinds.register();

		// Drawn top to bottom in this order; the HUD editor shows them all.
		HudManager.add(Events.hud());
		HudManager.add(Commissions.hud());
		HudManager.add(PowderTracker.hud());
		HudManager.add(Tools.hud());
		HudManager.add(MiningInfo.hud());
		HudManager.add(Markers.hud());
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Branding.MOD_ID, "hud"),
				(g, delta) -> Safe.run("HUD", () -> HudManager.render(g, delta)));
		// Cutscenes (corpse roulette, big drops) draw on top of everything.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Branding.MOD_ID, "cutscenes"),
				(g, delta) -> Safe.run("Cutscenes", () -> Cutscenes.render(g)));

		boolean showcase = DevShowcase.enabled();
		boolean showcaseWorld = DevShowcaseWorld.enabled();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (showcase) DevShowcase.tick(client);
			if (showcaseWorld) DevShowcaseWorld.tick(client);
			if (openNextTick != null) {
				client.gui.setScreen(openNextTick.get());
				openNextTick = null;
			}
			if (client.player == null) return;
			// Every tick (things drawn in the world are drawn per tick). Each feature is wrapped
			// so an error pauses only that feature instead of crashing the game.
			Safe.run("Keybinds", () -> Keybinds.tick(client));
			Safe.run("Waypoints", () -> Markers.tick(client));
			Safe.run("Best next block", () -> BestBlock.tick(client));
			Safe.run("Mob outlines", () -> MobOutlines.tick(client));
			Safe.run("Titanium alert", () -> TitaniumAlert.tick(client));
			Safe.run("Pickobulus preview", () -> PickobulusPreview.tick(client));
			Safe.run("Puzzler solver", () -> PuzzlerSolver.tick(client));
			if (++ticks < UPDATE_INTERVAL) return;
			ticks = 0;
			Safe.run("Reading Hypixel", SkyBlockState::update);
			Safe.run("Welcome", Welcome::tick);
			Safe.run("Commissions", Commissions::tick);
			Safe.run("Waypoints", Markers::updateAuto);
			Safe.run("Forge reminder", ForgeReminder::tick);
			Safe.run("Powder tracker", PowderTracker::tick);
			Safe.run("Events", Events::tick);
			Safe.run("Tools", Tools::tick);
			Safe.run("Mining info", MiningInfo::tick);
			Safe.run("Corpse loot", CorpseLoot::tick);
			Safe.run("Learning item icons", () -> SkyBlockItems.learn(client.player));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> SkyMoleCommand.register(dispatcher));

		LOGGER.info("{} loaded", Branding.NAME);
	}
}
