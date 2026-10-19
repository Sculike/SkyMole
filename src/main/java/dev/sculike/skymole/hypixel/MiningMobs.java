package dev.sculike.skymole.hypixel;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Mobs that live on the mining islands, recognized by the name tag Hypixel floats above them
 * (e.g. "[Lv6] Goblin 100/100❤"), each with an outline color.
 */
public final class MiningMobs {
	public record Mob(String name, int color) {}

	private record Entry(Pattern pattern, Mob mob) {}

	private static Entry e(String name, int color) {
		return new Entry(Pattern.compile("\\b" + Pattern.quote(name) + "\\b", Pattern.CASE_INSENSITIVE), new Mob(name, color));
	}

	// Specific names first ("Golden Goblin" before "Goblin").
	private static final List<Entry> MOBS = List.of(
			// Dwarven Mines
			e("Golden Goblin", 0xFFFFC93C),
			e("Diamond Goblin", 0xFF4FC9DE),
			e("Goblin", 0xFF7BD88F),
			e("Weakling", 0xFF7BD88F),
			e("Knifethrower", 0xFF7BD88F),
			e("Fireslinger", 0xFF7BD88F),
			e("Glacite Walker", 0xFF9FD8FF),
			e("Ice Walker", 0xFF9FD8FF),
			e("Treasure Hoarder", 0xFFFFB347),
			e("Star Sentry", 0xFFC77DFF),
			e("Powder Ghast", 0xFFF29BB0),
			e("Ghost", 0xFFF4EDE4),
			// Crystal Hollows
			e("Automaton", 0xFFA9BCC2),
			e("Sludge", 0xFF9BE15D),
			e("Yog", 0xFFFF5C5C),
			e("Thyst", 0xFFC77DFF),
			e("Grunt", 0xFFFFB347),
			e("Sergeant", 0xFFFFB347),
			e("Wendy", 0xFFFFB347),
			e("Viper", 0xFFFFB347),
			e("Corleone", 0xFFFFC93C),
			e("Worm", 0xFFF29BB0),
			e("Scatha", 0xFFF29BB0),
			// Deep Caverns
			e("Emerald Slime", 0xFF2ECC71),
			e("Lapis Zombie", 0xFF4A7BFF),
			e("Redstone Pigman", 0xFFFF5C5C),
			e("Miner Zombie", 0xFFA9BCC2),
			e("Miner Skeleton", 0xFFA9BCC2),
			e("Sneaky Creeper", 0xFF7BD88F),
			// Glacite Mineshafts
			e("Maniac", 0xFFFF5C5C));

	/** NPCs at The Forge, outlined while you're there. */
	private static final Pattern FORGE_NPC = Pattern.compile("\\bForge", Pattern.CASE_INSENSITIVE);

	private MiningMobs() {}

	/** The mining mob a name tag belongs to, or null. */
	public static Mob match(String nameTag) {
		for (Entry entry : MOBS) {
			if (entry.pattern().matcher(nameTag).find()) return entry.mob();
		}
		return null;
	}

	/** True for name tags of Forge NPCs ("Forge Foreman", "Forger", ...). */
	public static boolean isForgeNpc(String nameTag) {
		return FORGE_NPC.matcher(nameTag).find();
	}
}
