package dev.sculike.skymole.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;

import dev.sculike.skymole.SkyMole;
import dev.sculike.skymole.config.SkyMoleConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

/**
 * Short commands for long Hypixel ones, e.g. /don instead of /tptodonexpresso.
 * They only send the command you typed; nothing runs on its own.
 */
public final class Aliases {
	private Aliases() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		for (Map.Entry<String, String> alias : SkyMoleConfig.get().commands.aliases.entrySet()) {
			String shortName = alias.getKey().trim();
			String full = alias.getValue().trim();
			if (shortName.isEmpty() || full.isEmpty() || shortName.contains(" ")) continue;
			try {
				dispatcher.register(literal(shortName).executes(ctx -> {
					send(full);
					return 1;
				}));
			} catch (RuntimeException e) {
				SkyMole.LOGGER.warn("Couldn't add the /{} shortcut: {}", shortName, e.toString());
			}
		}
	}

	/** Sends it to Hypixel exactly as if you had typed it. */
	private static void send(String command) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection != null) connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
	}
}
