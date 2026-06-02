package org.dawnteam.accessibility.gui;

import net.minecraft.world.item.ItemStack;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class HotbarItemReader {
	private String lastItemName = "";
	private String currentItemName = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;

	public void update(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			if (!currentItemName.isEmpty()) reset();
			return;
		}

		String itemName = stack.getHoverName().getString();
		if (!itemName.equals(lastItemName)) {
			lastItemName = itemName;
			currentItemName = itemName;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrent = false;
		}

		if (!spokenForCurrent
				&& DawnAccessibilityClient.config().isHotbarReaderEnabled()
				&& DawnAccessibilityClient.config().isEnabled()
				&& System.currentTimeMillis() - hoverStartedAtMs >= DawnAccessibilityClient.config().getHotbarDelayMs()) {
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
