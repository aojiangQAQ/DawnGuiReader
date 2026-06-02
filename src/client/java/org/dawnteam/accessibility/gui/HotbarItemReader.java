package org.dawnteam.accessibility.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class HotbarItemReader {
	private int lastSelectedSlot = -1;
	private String lastItemName = "";
	private String currentItemName = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;

	public void update(ItemStack stack, int selectedSlot) {
		var cfg = DawnAccessibilityClient.config();
		if (!cfg.isHotbarReaderEnabled() || !cfg.isEnabled()) {
			if (!currentItemName.isEmpty()) reset();
			return;
		}

		String itemName;
		if (stack == null || stack.isEmpty()) {
			itemName = Component.translatable("message.dawn_accessibility.empty_hand").getString();
		} else {
			itemName = stack.getHoverName().getString();
		}

		// Detect slot change OR item name change
		if (selectedSlot != lastSelectedSlot || !itemName.equals(lastItemName)) {
			lastSelectedSlot = selectedSlot;
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
		lastSelectedSlot = -1;
		lastItemName = "";
		currentItemName = "";
		hoverStartedAtMs = 0L;
		spokenForCurrent = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}