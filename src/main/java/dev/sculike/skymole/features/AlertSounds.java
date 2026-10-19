package dev.sculike.skymole.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** The alert sounds you can pick in the menu. */
public final class AlertSounds {
	public static final String[] NAMES = {
			"Pling", "Bell", "Chime", "Harp", "Xylophone", "Retro", "Amethyst", "XP orb", "Level up", "Villager", "Anvil", "None"};

	private AlertSounds() {}

	public static void play(int index, int volumePercent) {
		float volume = volumePercent / 100f;
		if (volume <= 0) return;
		SoundEvent sound;
		float pitch = 1.0f;
		switch (index) {
			case 0 -> { sound = SoundEvents.NOTE_BLOCK_PLING.value(); pitch = 2.0f; }
			case 1 -> { sound = SoundEvents.NOTE_BLOCK_BELL.value(); pitch = 1.5f; }
			case 2 -> { sound = SoundEvents.NOTE_BLOCK_CHIME.value(); pitch = 1.5f; }
			case 3 -> { sound = SoundEvents.NOTE_BLOCK_HARP.value(); pitch = 2.0f; }
			case 4 -> { sound = SoundEvents.NOTE_BLOCK_XYLOPHONE.value(); pitch = 1.5f; }
			case 5 -> { sound = SoundEvents.NOTE_BLOCK_BIT.value(); pitch = 1.5f; }
			case 6 -> sound = SoundEvents.AMETHYST_BLOCK_CHIME;
			case 7 -> sound = SoundEvents.EXPERIENCE_ORB_PICKUP;
			case 8 -> sound = SoundEvents.PLAYER_LEVELUP;
			case 9 -> sound = SoundEvents.VILLAGER_YES;
			case 10 -> sound = SoundEvents.ANVIL_LAND;
			default -> { return; }
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}
}
