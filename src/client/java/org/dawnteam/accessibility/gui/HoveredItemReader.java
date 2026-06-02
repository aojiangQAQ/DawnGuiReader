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
			spokenForCurrentHover = false;
			detailSpoken = false;
		}

		long elapsed = System.currentTimeMillis() - hoverStartedAtMs;

		if (!spokenForCurrentHover && elapsed >= cfg.getHoverDelayMs()) {
			spokenForCurrentHover = true;
			DawnAccessibilityClient.speak(itemName);
		}

		if (!detailSpoken && spokenForCurrentHover
				&& cfg.isTooltipDetailEnabled()
				&& elapsed >= cfg.getHoverDelayMs() + cfg.getTooltipDetailDelayMs()) {
			detailSpoken = true;
			speakDetail(stack);
		}
	}

	private void speakDetail(ItemStack stack) {
		// Get the mod name from registry namespace (the blue text in creative tooltips)
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String namespace = id.getNamespace();
		// Translate common namespaces to friendly names
		String modName = switch (namespace) {
			case "minecraft" -> "Minecraft";
			default -> {
				// Try to get display name from Fabric mod loader
				try {
					var modOpt = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(namespace);
					if (modOpt.isPresent()) {
						yield modOpt.get().getMetadata().getName();
					}
				} catch (Exception ignored) {}
				// Fallback: capitalize namespace
				yield namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
			}
		};
		if (modName != null && !modName.isEmpty()) {
			DawnAccessibilityClient.speak(modName);
		}
	}

	public void reset() {
		lastSlotId = -1; lastItemName = ""; currentItemName = "";
		hoverStartedAtMs = 0L; spokenForCurrentHover = false; detailSpoken = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}