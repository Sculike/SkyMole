package dev.sculike.skymole.hypixel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * Hands every chat message (as plain text) to the features that listen for one,
 * and keeps the last messages so /skymole debug can show what Hypixel sent.
 */
public final class Chat {
	private static final int KEEP = 60;
	private static final List<Consumer<String>> LISTENERS = new ArrayList<>();
	private static final Deque<String> RECENT = new ArrayDeque<>();

	private Chat() {}

	public static void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return; // action bar, not chat
			String text = TabList.clean(message.getString()).trim();
			if (text.isEmpty()) return;
			synchronized (RECENT) {
				RECENT.addLast(text);
				while (RECENT.size() > KEEP) RECENT.removeFirst();
			}
			// Each listener separately: one broken chat feature doesn't pause the others.
			for (Consumer<String> listener : LISTENERS) {
				dev.sculike.skymole.Safe.run("Chat " + listener.getClass().getName(), () -> listener.accept(text));
			}
		});
	}

	public static void listen(Consumer<String> listener) {
		LISTENERS.add(listener);
	}

	public static List<String> recent() {
		synchronized (RECENT) {
			return new ArrayList<>(RECENT);
		}
	}
}
