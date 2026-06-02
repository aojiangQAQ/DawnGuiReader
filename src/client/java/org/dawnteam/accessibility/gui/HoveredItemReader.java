package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.List;
import java.util.Optional;

public final class HoveredItemReader {
	private int lastSlotId = -1;
	private String lastItemName = "";
	private String currentItemName = "";
	private ItemStack currentStack = ItemStack.EMPTY;
	private long hoverStartedAtMs;
	private boolean spokenForCurrentHover;
	private boolean tooltipSpoken;

	public void update(Slot hoveredSlot) {
		if (hoveredSlot == null || !hoveredSlot.hasItem()) {
			reset();
			return;
		}

		var cfg = DawnAccessibilityClient.config();
		if (!cfg.isContainerReaderEnabled() || !cfg.isEnabled()) {
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
			currentStack = stack;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrentHover = false;
			tooltipSpoken = false;
		}

		long elapsed = System.currentTimeMillis() - hoverStartedAtMs;

		if (!spokenForCurrentHover && elapsed >= cfg.getHoverDelayMs()) {
			spokenForCurrentHover = true;
			DawnAccessibilityClient.speak(itemName);
		}

		if (!tooltipSpoken && spokenForCurrentHover
				&& cfg.isTooltipDetailEnabled()
				&& elapsed >= cfg.getHoverDelayMs() + cfg.getTooltipDetailDelayMs()) {
			tooltipSpoken = true;
			speakTooltip();
		}
	}

	private void speakTooltip() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || currentStack.isEmpty()) return;
		// Use creative flag to get blue description text (mod name, category etc.)
		Item.TooltipContext tooltipContext = Item.TooltipContext.of(client.level);
		TooltipFlag.Default flag = new TooltipFlag.Default(false, true);
		List<Component> lines = currentStack.getTooltipLines(tooltipContext, client.player, flag);
		StringBuilder sb = new StringBuilder();
		// Skip line 0 (item name), read all other lines
		for (int i = 1; i < lines.size(); i++) {
			String t = lines.get(i).getString().trim();
			if (!t.isEmpty()) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(t);
			}
		}
		if (sb.length() > 0) DawnAccessibilityClient.speak(sb.toString());
	}

	public void reset() {
		lastSlotId = -1; lastItemName = ""; currentItemName = "";
		currentStack = ItemStack.EMPTY;
		hoverStartedAtMs = 0L; spokenForCurrentHover = false; tooltipSpoken = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}