package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class HoveredItemReader {
	private int lastSlotId = -1;
	private String lastItemName = "";
	private String currentItemName = "";
	private long hoverStartedAtMs;
	private long nameSpokenAtMs;
	private boolean spokenForCurrentHover;
	private boolean detailSpoken;

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
			hoverStartedAtMs = System.currentTimeMillis();
			nameSpokenAtMs = 0;
			spokenForCurrentHover = false;
			detailSpoken = false;
		}

		long now = System.currentTimeMillis();
		long elapsed = now - hoverStartedAtMs;

		if (!spokenForCurrentHover && elapsed >= cfg.getHoverDelayMs()) {
			spokenForCurrentHover = true;
			nameSpokenAtMs = now;
			DawnAccessibilityClient.speak(itemName);
		}

		if (!detailSpoken && cfg.isTooltipDetailEnabled() && spokenForCurrentHover) {
			boolean shouldSpeak;
			if (cfg.getTooltipDetailMode() == 1) {
				// Sequential mode: delay starts after item name was spoken
				shouldSpeak = now - nameSpokenAtMs >= cfg.getTooltipDetailDelayMs();
			} else {
				// Independent mode: delay starts from hover
				shouldSpeak = elapsed >= cfg.getHoverDelayMs() + cfg.getTooltipDetailDelayMs();
			}
			if (shouldSpeak) {
				detailSpoken = true;
				speakModName(stack);
			}
		}
	}

	private void speakModName(ItemStack stack) {
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String namespace = id.getNamespace();
		String modName = switch (namespace) {
			case "minecraft" -> "Minecraft";
			default -> {
				try {
					var modOpt = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(namespace);
					if (modOpt.isPresent()) yield modOpt.get().getMetadata().getName();
				} catch (Exception ignored) {}
				yield namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
			}
		};
		if (modName != null && !modName.isEmpty()) {
			DawnAccessibilityClient.speak(modName);
		}
	}

	public void reset() {
		lastSlotId = -1; lastItemName = ""; currentItemName = "";
		hoverStartedAtMs = 0L; nameSpokenAtMs = 0L;
		spokenForCurrentHover = false; detailSpoken = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}