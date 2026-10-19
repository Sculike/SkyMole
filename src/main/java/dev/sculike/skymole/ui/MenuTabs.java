package dev.sculike.skymole.ui;

import java.util.ArrayList;
import java.util.List;

import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.features.Alerts;
import dev.sculike.skymole.features.AlertSounds;
import dev.sculike.skymole.features.Commissions;
import dev.sculike.skymole.features.PowderTracker;
import dev.sculike.skymole.ui.MenuEntry.Button;
import dev.sculike.skymole.ui.MenuEntry.Cycle;
import dev.sculike.skymole.ui.MenuEntry.Description;
import dev.sculike.skymole.ui.MenuEntry.Header;
import dev.sculike.skymole.ui.MenuEntry.Slider;
import dev.sculike.skymole.ui.MenuEntry.Toggle;
import dev.sculike.skymole.command.SkyMoleCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

/** Everything in the SkyMole menu. Every row here changes a working feature. */
final class MenuTabs {
	private MenuTabs() {}

	static List<MenuTab> build(Runnable openHudEditor) {
		SkyMoleConfig c = SkyMoleConfig.get();
		SkyMoleConfig d = new SkyMoleConfig();
		SkyMoleConfig.Commissions cc = c.commissions;
		SkyMoleConfig.Powder pw = c.powder;
		SkyMoleConfig.Events ev = c.events;
		SkyMoleConfig.Tools tl = c.tools;
		SkyMoleConfig.Area ar = c.area;
		SkyMoleConfig.General gen = c.general;
		SkyMoleConfig.Markers mk = c.markers;

		List<MenuTab> tabs = new ArrayList<>();

		tabs.add(new MenuTab("Commissions", Items.DIAMOND_PICKAXE, List.of(
				new Header("Commission HUD"),
				new Description("Your commissions and progress in the mines."),
				new Toggle("Show commission HUD", () -> cc.hud.enabled, v -> cc.hud.enabled = v, d.commissions.hud.enabled),
				new Cycle("Progress display", SkyMoleConfig.Commissions.DISPLAY_MODES,
						() -> cc.display, v -> cc.display = v, d.commissions.display),
				new Slider("HUD scale", () -> cc.hud.scalePercent, v -> cc.hud.scalePercent = v,
						50, 300, 10, d.commissions.hud.scalePercent, "%"),
				new Header("Alerts"),
				new Toggle("Commission done alert", () -> cc.doneAlert, v -> cc.doneAlert = v, d.commissions.doneAlert),
				new Toggle("\"Go claim\" reminder when all are done", () -> cc.claimReminder,
						v -> cc.claimReminder = v, d.commissions.claimReminder),
				new Toggle("Chat message for new commissions", () -> cc.newCommissionMessage,
						v -> cc.newCommissionMessage = v, d.commissions.newCommissionMessage),
				new Button("Test alert", () -> Alerts.show("Commission done!", "Example Commission")),
				new Header("Waypoints"),
				new Description("Right-click a commission (chat open or Commissions menu) for a route."),
				new Toggle("Commission waypoints", () -> mk.enabled, v -> mk.enabled = v, d.markers.enabled),
				new Toggle("Show waypoints for all active commissions", () -> mk.autoCommissions,
						v -> mk.autoCommissions = v, d.markers.autoCommissions),
				new Toggle("Route line to the right-clicked one", () -> mk.routeLine, v -> mk.routeLine = v,
						d.markers.routeLine),
				new Toggle("Hint under the commission HUD", () -> cc.waypointHint, v -> cc.waypointHint = v,
						d.commissions.waypointHint),
				new Toggle("Beam above the marker", () -> mk.beam, v -> mk.beam = v, d.markers.beam),
				new Toggle("Show marker HUD (arrow + distance)", () -> mk.hud.enabled, v -> mk.hud.enabled = v,
						d.markers.hud.enabled))));

		tabs.add(new MenuTab("Powder", Items.PRISMARINE_CRYSTALS, List.of(
				new Header("Powder tracker"),
				new Description("Powder gained this session and per hour."),
				new Toggle("Show powder HUD", () -> pw.hud.enabled, v -> pw.hud.enabled = v, d.powder.hud.enabled),
				new Toggle("Mithril powder", () -> pw.mithril, v -> pw.mithril = v, d.powder.mithril),
				new Toggle("Gemstone powder", () -> pw.gemstone, v -> pw.gemstone = v, d.powder.gemstone),
				new Toggle("Glacite powder", () -> pw.glacite, v -> pw.glacite = v, d.powder.glacite),
				new Toggle("Per-hour rates", () -> pw.rates, v -> pw.rates = v, d.powder.rates),
				new Slider("Pause clock when idle for", () -> pw.pauseAfterMinutes, v -> pw.pauseAfterMinutes = v,
						0, 10, 1, d.powder.pauseAfterMinutes, " min"),
				new Header("Counters"),
				new Toggle("Pristine procs", () -> pw.pristine, v -> pw.pristine = v, d.powder.pristine),
				new Toggle("Treasure chests", () -> pw.chests, v -> pw.chests = v, d.powder.chests),
				new Button("Reset session", () -> {
					PowderTracker.reset();
					Alerts.chat("Powder session reset.");
				}))));

		tabs.add(new MenuTab("Events", Items.CLOCK, List.of(
				new Header("Event timers"),
				new Description("Mining events in your lobby, from the boss bar."),
				new Toggle("Show events HUD", () -> ev.hud.enabled, v -> ev.hud.enabled = v, d.events.hud.enabled),
				new Toggle("Alert when an event starts", () -> ev.startAlert, v -> ev.startAlert = v, d.events.startAlert),
				new Toggle("Message when an event ends", () -> ev.endAlert, v -> ev.endAlert = v, d.events.endAlert),
				new Header("Goblins"),
				new Toggle("Golden and Diamond Goblin alert", () -> ev.goblinAlert,
						v -> ev.goblinAlert = v, d.events.goblinAlert),
				new Toggle("Powder Ghast alert (click to mark)", () -> ev.powderGhastAlert,
						v -> ev.powderGhastAlert = v, d.events.powderGhastAlert))));

		tabs.add(new MenuTab("Tools", Items.GOLDEN_PICKAXE, List.of(
				new Header("Tools HUD"),
				new Toggle("Show tools HUD", () -> tl.hud.enabled, v -> tl.hud.enabled = v, d.tools.hud.enabled),
				new Header("Pickaxe ability"),
				new Toggle("Show ability cooldown", () -> tl.ability, v -> tl.ability = v, d.tools.ability),
				new Toggle("Alert when your ability is ready", () -> tl.abilityReadyAlert,
						v -> tl.abilityReadyAlert = v, d.tools.abilityReadyAlert),
				new Toggle("Pickobulus preview when ready", () -> tl.pickobulusPreview, v -> tl.pickobulusPreview = v,
						d.tools.pickobulusPreview),
				new Description("Shows the blocks it will break; calibrates itself after a throw."),
				new Header("Drill"),
				new Toggle("Show drill fuel", () -> tl.fuel, v -> tl.fuel = v, d.tools.fuel),
				new Slider("Low fuel warning below", () -> tl.lowFuelPercent, v -> tl.lowFuelPercent = v,
						0, 50, 5, d.tools.lowFuelPercent, "%"),
				new Header("Stats"),
				new Description("From the tab list's Stats widget."),
				new Toggle("Mining speed and fortune", () -> tl.stats, v -> tl.stats = v, d.tools.stats),
				new Header("Titanium"),
				new Toggle("Titanium alert", () -> tl.titaniumAlert, v -> tl.titaniumAlert = v, d.tools.titaniumAlert),
				new Toggle("Outline new Titanium", () -> tl.titaniumOutline, v -> tl.titaniumOutline = v,
						d.tools.titaniumOutline),
				new Header("Forge"),
				new Description("Reads the Forge widget and the Forge menu; works after restarts too."),
				new Toggle("Forge ready reminder", () -> c.forge.reminder, v -> c.forge.reminder = v, d.forge.reminder),
				new Toggle("Forge timers on the Tools HUD", () -> c.forge.hud, v -> c.forge.hud = v, d.forge.hud),
				new Header("Best next block"),
				new Description("Green: mine this now. Orange: mine this next."),
				new Toggle("Highlight best next block", () -> tl.bestBlock, v -> tl.bestBlock = v, d.tools.bestBlock),
				new Toggle("Show the next block (orange)", () -> tl.bestBlockSecond, v -> tl.bestBlockSecond = v,
						d.tools.bestBlockSecond),
				new Description("Spread is read from SkyBlock Menu > Your SkyBlock Profile."),
				new Slider("Mining Spread if not detected", () -> tl.manualSpread, v -> tl.manualSpread = v,
						0, 500, 10, d.tools.manualSpread, ""))));

		tabs.add(new MenuTab("Crystal Hollows", Items.AMETHYST_SHARD, List.of(
				new Header("Mining info HUD"),
				new Description("Zone, crystals, lobby day and worms."),
				new Toggle("Show mining info HUD", () -> ar.hud.enabled, v -> ar.hud.enabled = v, d.area.hud.enabled),
				new Toggle("Zone name", () -> ar.zone, v -> ar.zone = v, d.area.zone),
				new Toggle("Crystals found", () -> ar.crystals, v -> ar.crystals = v, d.area.crystals),
				new Toggle("Lobby day", () -> ar.lobbyDay, v -> ar.lobbyDay = v, d.area.lobbyDay),
				new Header("Scatha"),
				new Toggle("Worm spawn alert", () -> ar.wormAlert, v -> ar.wormAlert = v, d.area.wormAlert),
				new Toggle("Worm cooldown timer", () -> ar.wormCooldown, v -> ar.wormCooldown = v, d.area.wormCooldown))));

		tabs.add(new MenuTab("Glacite", Items.BLUE_ICE, List.of(
				new Header("Glacite Tunnels"),
				new Toggle("Show cold", () -> ar.cold, v -> ar.cold = v, d.area.cold),
				new Slider("Cold warning at", () -> ar.coldWarning, v -> ar.coldWarning = v,
						0, 95, 5, d.area.coldWarning, ""),
				new Header("Mineshafts"),
				new Toggle("Mineshaft found alert", () -> ar.mineshaftAlert,
						v -> ar.mineshaftAlert = v, d.area.mineshaftAlert))));

		tabs.add(new MenuTab("Mobs", Items.IRON_SWORD, List.of(
				new Header("Mob outlines"),
				new Description("Outlines only show where you can see the mob, never through walls."),
				new Toggle("Outline mining mobs", () -> ar.mobOutlines, v -> ar.mobOutlines = v, d.area.mobOutlines),
				new Description("Goblins, Walkers, Hollows mobs, Emerald Slimes and more."),
				new Toggle("Outline Forge NPCs at the Forge", () -> ar.forgeOutline, v -> ar.forgeOutline = v,
						d.area.forgeOutline))));

		tabs.add(new MenuTab("Solvers", Items.BOOK, List.of(
				new Header("Daily"),
				new Toggle("Fetchur answer in chat", () -> c.solvers.fetchur, v -> c.solvers.fetchur = v, d.solvers.fetchur),
				new Toggle("Puzzler solver (outlines the block)", () -> c.solvers.puzzler, v -> c.solvers.puzzler = v,
						d.solvers.puzzler),
				new Toggle("Sky Mall perk on the HUD", () -> ar.skyMall, v -> ar.skyMall = v, d.area.skyMall))));

		SkyMoleConfig.Fun fun = c.fun;
		tabs.add(new MenuTab("Fun", Items.FIREWORK_ROCKET, List.of(
				new Header("Corpse roulette"),
				new Description("A case-opening spin when you loot a Mineshaft corpse."),
				new Toggle("Corpse roulette", () -> fun.corpseRoulette, v -> fun.corpseRoulette = v, d.fun.corpseRoulette),
				new Cycle("Speed", new String[] {"Normal", "Fast"}, () -> fun.rouletteSpeed, v -> fun.rouletteSpeed = v,
						d.fun.rouletteSpeed),
				new Button("Try the roulette", () -> {
					Minecraft.getInstance().gui.setScreen(null);
					SkyMoleCommand.testRoulette();
				}),
				new Header("Big drop"),
				new Description("A celebration when a corpse drop is worth a lot."),
				new Toggle("Big drop animation", () -> fun.bigDrop, v -> fun.bigDrop = v, d.fun.bigDrop),
				new Slider("Celebrate drops worth at least", () -> fun.bigDropMillions, v -> fun.bigDropMillions = v,
						1, 100, 1, d.fun.bigDropMillions, "m"),
				new Button("Try the big drop", () -> {
					Minecraft.getInstance().gui.setScreen(null);
					SkyMoleCommand.testBigDrop();
				}))));

		tabs.add(new MenuTab("General", Items.COMPARATOR, List.of(
				new Header("HUD"),
				new Button("Edit HUD positions", openHudEditor),
				new Slider("Background opacity", () -> gen.backgroundOpacityPercent,
						v -> gen.backgroundOpacityPercent = v, 0, 100, 5, d.general.backgroundOpacityPercent, "%"),
				new Toggle("Text shadow", () -> gen.textShadow, v -> gen.textShadow = v, d.general.textShadow),
				new Header("Alerts"),
				new Description("Used by every SkyMole alert."),
				new Cycle("Alert sound", AlertSounds.NAMES, () -> gen.alertSound, v -> gen.alertSound = v,
						d.general.alertSound).onChange(v -> AlertSounds.play(v, gen.volumePercent)),
				new Slider("Volume", () -> gen.volumePercent, v -> gen.volumePercent = v,
						0, 100, 5, d.general.volumePercent, "%"),
				new Button("Test alert", () -> Alerts.show("Test alert", "This is how alerts look")),
				new Header("Menu"),
				new Cycle("Menu background", new String[] {"Light tint", "Game blur", "None"},
						() -> gen.menuBackground, v -> gen.menuBackground = v, d.general.menuBackground))));

		return tabs;
	}
}
