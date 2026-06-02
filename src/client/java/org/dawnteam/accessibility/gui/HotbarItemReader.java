package org.dawnteam.accessibility.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class HotbarItemReader {
	private String lastItemName = "";
	private String currentItemName = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;

	public void update(ItemStack stack) {
		var cfg = DawnAccessibilityClient.config();
		if (!cfg.isHotbarReaderEnabled() || !cfg.isEnabled()) {
			if (!currentItemName.isEmpty()) reset();
			return;
		}

		// Empty hand: read "hand" translation
		String itemName;
		if (stack == null || stack.isEmpty()) {
			itemName = Component.translatable("message.dawn_accessibility.empty_hand").getString();
		} else {
			itemName = stack.getHoverName().getString();
		}

		if (!itemName.equals(lastItemName)) {
			lastItemName = itemName;
			currentItemName = itemName;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrent = false;
		}

		if (!spokenForCurrent
				&& System.currentTimeMillis() - hoverStartedAtMs >= cfg.getHotbarDelayMs()) {
			spokenForCurrent = true;
			DawnAccessibilityClient.speak(itemName);
		}
	}

	public void reset() {
		lastItemName = "";
		currentItemName = "";
		hoverStartedAtMs = 0L;
		spokenForCurrent = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}