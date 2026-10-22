package dev.sculike.skymole.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.sculike.skymole.Branding;
import dev.sculike.skymole.SkyMole;
import dev.sculike.skymole.config.SkyMoleConfig;
import dev.sculike.skymole.features.CorpseLoot;
import dev.sculike.skymole.features.Markers;
import dev.sculike.skymole.features.PowderTracker;
import dev.sculike.skymole.features.UserRoutes;
import dev.sculike.skymole.hud.HudEditorScreen;
import dev.sculike.skymole.hud.HudManager;
import dev.sculike.skymole.hypixel.Chat;
import dev.sculike.skymole.hypixel.Sidebar;
import dev.sculike.skymole.hypixel.SkyBlockState;
import dev.sculike.skymole.hypixel.TabList;
import dev.sculike.skymole.mixin.BossHealthOverlayAccessor;
import dev.sculike.skymole.ui.MenuScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** /skymole opens the menu; subcommands are shortcuts and debugging help. */
public final class SkyMoleCommand {
	private SkyMoleCommand() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("skymole")
				.executes(ctx -> {
					SkyMole.openNextTick(MenuScreen::new);
					return 1;
				})
				.then(literal("help").executes(SkyMoleCommand::help))
				.then(literal("hud").executes(ctx -> {
					SkyMole.openNextTick(() -> new HudEditorScreen(null));
					return 1;
				}))
				.then(literal("preview").executes(ctx -> {
					boolean on = HudManager.togglePreview();
					return say(ctx, on ? "Preview on: every HUD shows example data." : "Preview off.");
				}))
				.then(literal("powder").then(literal("reset").executes(ctx -> {
					PowderTracker.reset();
					return say(ctx, "Powder session reset.");
				})))
				.then(literal("marker")
						.then(literal("clear").executes(ctx -> {
							Markers.clear();
							return say(ctx, "Marker removed.");
						}))
						.then(literal("set").then(argument("name", StringArgumentType.greedyString())
								.suggests(SkyMoleCommand::suggestSpots).executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									var pos = Markers.setCustom(name);
									if (pos == null) return say(ctx, "Go to a mining island first.");
									return say(ctx, name + " is now at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".");
								})))
						.then(literal("reset").then(argument("name", StringArgumentType.greedyString())
								.suggests(SkyMoleCommand::suggestSpots).executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									return say(ctx, Markers.removeCustom(name)
											? name + " is back to the built-in spot." : "You hadn't moved " + name + ".");
								})))
						.then(argument("name", StringArgumentType.greedyString())
								.suggests(SkyMoleCommand::suggestSpots).executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									return say(ctx, Markers.markByName(name)
											? "Marked " + name + "." : "No spot called " + name + " on this island.");
								})))
				.then(literal("route")
						.executes(SkyMoleCommand::listRoutes)
						.then(literal("record").then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
							String name = StringArgumentType.getString(ctx, "name").trim();
							if (!UserRoutes.start(name)) {
								return say(ctx, "Already recording " + UserRoutes.recordingName()
										+ ". Finish it with /skymole route save.");
							}
							return say(ctx, "Recording \"" + name + "\". Walk the route; points drop as you go. "
									+ "/skymole route save when you're done.");
						})))
						.then(literal("point").executes(ctx -> {
							int n = UserRoutes.addPoint();
							return say(ctx, n < 0 ? "You're not recording a route."
									: "Point " + n + " added where you're standing.");
						}))
						.then(literal("save").executes(ctx -> {
							int n = UserRoutes.stop();
							if (n < 0) return say(ctx, "You're not recording a route.");
							if (n == 0) return say(ctx, "That route had nothing in it, so nothing was saved.");
							return say(ctx, "Saved with " + n + " points. It's showing now.");
						}))
						.then(literal("cancel").executes(ctx -> {
							UserRoutes.cancel();
							return say(ctx, "Recording thrown away.");
						}))
						.then(literal("hide").executes(ctx -> {
							UserRoutes.hide();
							return say(ctx, "Route hidden.");
						}))
						.then(literal("delete").then(argument("name", StringArgumentType.greedyString())
								.suggests(SkyMoleCommand::suggestRoutes).executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									return say(ctx, UserRoutes.delete(name)
											? "Deleted " + name + "." : "There's no route called " + name + ".");
								})))
						.then(argument("name", StringArgumentType.greedyString())
								.suggests(SkyMoleCommand::suggestRoutes).executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									return say(ctx, UserRoutes.show(name)
											? "Showing " + name + "." : "There's no route called " + name + ".");
								})))
				.then(literal("test")
						.then(literal("roulette").executes(ctx -> {
							testRoulette();
							return 1;
						}))
						.then(literal("bigdrop").executes(ctx -> {
							testBigDrop();
							return 1;
						})))
				.then(literal("alias")
						.executes(SkyMoleCommand::listAliases)
						.then(literal("add").then(argument("short", StringArgumentType.word())
								.then(argument("command", StringArgumentType.greedyString()).executes(ctx -> {
									String shortName = StringArgumentType.getString(ctx, "short").toLowerCase(Locale.ROOT);
									String full = StringArgumentType.getString(ctx, "command").trim();
									SkyMoleConfig.get().commands.aliases.put(shortName, full);
									SkyMoleConfig.save();
									return say(ctx, "/" + shortName + " now runs /" + full
											+ ". It works after you rejoin the server.");
								}))))
						.then(literal("remove").then(argument("short", StringArgumentType.word()).executes(ctx -> {
							String shortName = StringArgumentType.getString(ctx, "short").toLowerCase(Locale.ROOT);
							boolean removed = SkyMoleConfig.get().commands.aliases.remove(shortName) != null;
							if (removed) SkyMoleConfig.save();
							return say(ctx, removed ? "/" + shortName + " removed. Gone after you rejoin."
									: "There's no /" + shortName + " shortcut.");
						}))))
				.then(literal("debug").executes(SkyMoleCommand::debug)));
	}

	private static int listAliases(CommandContext<FabricClientCommandSource> ctx) {
		var aliases = SkyMoleConfig.get().commands.aliases;
		if (aliases.isEmpty()) return say(ctx, "No shortcuts. Add one with /skymole alias add <short> <command>");
		say(ctx, "Shortcuts:");
		aliases.forEach((shortName, full) -> helpLine(ctx, "/" + shortName, "runs /" + full));
		return 1;
	}

	/** Example Lapis corpse loot, to preview the roulette without finding a corpse. */
	public static void testRoulette() {
		List<CorpseLoot.Reward> loot = List.of(
				new CorpseLoot.Reward("Glacite Powder", 1050, -1),
				new CorpseLoot.Reward("Enchanted Lapis Lazuli", 16, dev.sculike.skymole.hypixel.Prices.of("Enchanted Lapis Lazuli") * 16),
				new CorpseLoot.Reward("Skeleton Key", 1, dev.sculike.skymole.hypixel.Prices.of("Skeleton Key")));
		boolean fast = dev.sculike.skymole.config.SkyMoleConfig.get().fun.rouletteSpeed == 1;
		CorpseLoot.Reward best = loot.get(2);
		dev.sculike.skymole.ui.Cutscenes.roulette("Lapis", loot, best, fast);
	}

	/** Preview of the big-drop celebration. */
	public static void testBigDrop() {
		// The live price once prices have loaded, otherwise an example value.
		double live = dev.sculike.skymole.hypixel.Prices.of("Shattered Locket");
		dev.sculike.skymole.ui.Cutscenes.bigDrop("Shattered Locket", live > 0 ? live : 473_990_000);
	}

	private static CompletableFuture<Suggestions> suggestSpots(CommandContext<FabricClientCommandSource> ctx,
			SuggestionsBuilder builder) {
		String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (String name : Markers.names()) {
			if (name.toLowerCase(Locale.ROOT).startsWith(typed)) builder.suggest(name);
		}
		return builder.buildFuture();
	}

	private static int listRoutes(CommandContext<FabricClientCommandSource> ctx) {
		if (UserRoutes.names().isEmpty()) {
			say(ctx, "No routes yet. Walk one with /skymole route record <name>.");
			return 1;
		}
		say(ctx, "Your routes (click one to show it):");
		for (String name : UserRoutes.names()) {
			String island = UserRoutes.islandOf(name);
			helpLine(ctx, "/skymole route " + name, island.isEmpty() ? "" : "on " + island);
		}
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestRoutes(CommandContext<FabricClientCommandSource> ctx,
			SuggestionsBuilder builder) {
		for (String name : UserRoutes.suggestions(builder.getRemaining())) builder.suggest(name);
		return builder.buildFuture();
	}

	private static int help(CommandContext<FabricClientCommandSource> ctx) {
		say(ctx, "Commands (click one to use it):");
		helpLine(ctx, "/skymole", "open the settings menu");
		helpLine(ctx, "/skymole hud", "move and resize the HUD panels");
		helpLine(ctx, "/skymole preview", "show every HUD with example data");
		helpLine(ctx, "/skymole powder reset", "start a new powder session");
		helpLine(ctx, "/skymole marker ", "<spot>  mark a spot, e.g. Fetchur or Royal Mines");
		helpLine(ctx, "/skymole marker set ", "<spot>  move a spot to where you stand");
		helpLine(ctx, "/skymole marker clear", "remove the marker");
		helpLine(ctx, "/skymole route record ", "<name>  walk a route and save it");
		helpLine(ctx, "/skymole route", "list your routes, or show one");
		helpLine(ctx, "/skymole alias", "short commands, e.g. /don for /tptodonexpresso");
		helpLine(ctx, "/skymole debug", "save what SkyMole reads, for bug reports");
		return 1;
	}

	/** One help line: gold command, gray explanation. Clicking puts the command in chat. */
	private static void helpLine(CommandContext<FabricClientCommandSource> ctx, String command, String explanation) {
		MutableComponent line = Component.literal("  " + command).withColor(Branding.LAMP_GOLD & 0xFFFFFF)
				.append(Component.literal("  " + explanation).withColor(Branding.TEXT_DIM & 0xFFFFFF))
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.SuggestCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to type " + command))));
		ctx.getSource().sendFeedback(line);
	}

	private static int debug(CommandContext<FabricClientCommandSource> ctx) {
		List<String> out = new ArrayList<>();
		out.add("In SkyBlock: " + SkyBlockState.inSkyBlock());
		out.add("Area: " + SkyBlockState.area() + "  Zone: " + SkyBlockState.zone() + "  Cold: " + SkyBlockState.cold());
		out.add("Mining island: " + SkyBlockState.island());
		out.add("Commissions widget found: " + SkyBlockState.commissionWidgetFound());
		out.add("Commissions: " + SkyBlockState.commissions());
		out.add("Powders: " + SkyBlockState.powders());
		var tools = dev.sculike.skymole.config.SkyMoleConfig.get().tools;
		out.add("Spread: mining=" + dev.sculike.skymole.hypixel.SpreadStats.forOre("Mithril")
				+ " gemstone=" + dev.sculike.skymole.hypixel.SpreadStats.forOre("Ruby")
				+ " (seen in menu: " + tools.miningSpreadSeen + " / " + tools.gemstoneSpreadSeen
				+ ", manual: " + tools.manualSpread + ")");
		out.add("Forge: " + dev.sculike.skymole.features.ForgeReminder.describe());
		out.add("Prices: " + dev.sculike.skymole.hypixel.Prices.describe()
				+ "  Paused after errors: " + dev.sculike.skymole.Safe.paused());
		out.add("Routes: " + dev.sculike.skymole.features.UserRoutes.describe());
		out.add("Best block: on=" + tools.bestBlock + " showOrange=" + tools.bestBlockSecond + " "
				+ dev.sculike.skymole.features.BestBlock.describe());
		out.add("");
		out.add("=== Tab list widgets");
		for (Map.Entry<String, List<String>> e : SkyBlockState.sections().entrySet()) out.add(e.getKey() + ": " + e.getValue());
		out.add("");
		out.add("=== Boss bars");
		var overlay = (BossHealthOverlayAccessor) Minecraft.getInstance().gui.hud.getBossOverlay();
		overlay.skymole$events().values().forEach(bar -> out.add(TabList.clean(bar.getName().getString())));
		out.add("");
		out.add("=== Sidebar: " + Sidebar.title());
		out.addAll(Sidebar.lines());
		out.add("");
		out.add("=== Tab list (quotes show leading spaces)");
		for (String line : TabList.lines()) out.add("\"" + line + "\"");
		out.add("");
		out.add("=== Recent chat");
		out.addAll(Chat.recent());

		Path file = FabricLoader.getInstance().getConfigDir().resolve("skymole-debug.txt");
		try {
			Files.write(file, out);
		} catch (IOException e) {
			return say(ctx, "Couldn't write debug file: " + e.getMessage());
		}
		return say(ctx, "Saved details to config/skymole-debug.txt");
	}

	private static int say(CommandContext<FabricClientCommandSource> ctx, String message) {
		ctx.getSource().sendFeedback(Branding.chat(message));
		return 1;
	}
}
