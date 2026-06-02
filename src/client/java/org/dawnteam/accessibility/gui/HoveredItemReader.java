package org.dawnteam.accessibility.gui;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class HoveredItemReader {
	private int lastSlotId = -1;
	private String lastItemName = "";
	private String currentItemName = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrentHover;

	public void update(Slot hoveredSlot) {
		if (hoveredSlot == null || !hoveredSlot.hasItem()) {
			reset();
			return;
		}

		ItemStack stack = hoveredSlot.getItem();
		String itemName = stack.getHoverName().getString();
		int slotId = hoveredSlot.index;

		if (slotId != lastSlotId || !itemName.equals(lastItemName)) {
			lastSlotId = slotId;
			lastItemName = itemName;
			currentItemName = itemName;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrentHover = false;
		}

		if (!spokenForCurrentHover
				&& DawnAccessibilityClient.config().isEnabled()
				&& System.currentTimeMillis() - hoverStartedAtMs >= DawnAccessibilityClient.config().getHoverDelayMs()) {
			spokenForCurrentHover = true;
			DawnAccessibilityClient.speak(itemName);
		}
	}

	public void reset() {
		lastSlotId = -1;
		lastItemName = "";
		currentItemName = "";
		hoverStartedAtMs = 0L;
		spokenForCurrentHover = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}
