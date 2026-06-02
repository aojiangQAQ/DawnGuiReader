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
	private long estimatedSpeechEndMs;
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
			estimatedSpeechEndMs = 0;
			spokenForCurrentHover = false;
			detailSpoken = false;
		}

		long now = System.currentTimeMillis();
		long elapsed = now - hoverStartedAtMs;

		if (!spokenForCurrentHover && elapsed >= cfg.getHoverDelayMs()) {
			spokenForCurrentHover = true;
			nameSpokenAtMs = now;
			// Estimate speech duration: ~120ms per CJK char, ~80ms per Latin word
			int rate = cfg.getSpeechRate();
			double rateMultiplier = Math.max(0.3, 1.0 - rate * 0.08);
			long estimatedMs = estimateSpeechDuration(itemName, rateMultiplier);
			estimatedSpeechEndMs = now + estimatedMs;
			DawnAccessibilityClient.speak(itemName);
		}

		if (!detailSpoken && cfg.isTooltipDetailEnabled() && spokenForCurrentHover) {
			boolean shouldSpeak;
			if (cfg.getTooltipDetailMode() == 1) {
				// Sequential: delay starts AFTER estimated speech finishes
				shouldSpeak = now >= estimatedSpeechEndMs + cfg.getTooltipDetailDelayMs();
			} else {
				// Independent: delay starts from hover
				shouldSpeak = elapsed >= cfg.getHoverDelayMs() + cfg.getTooltipDetailDelayMs();
			}
			if (shouldSpeak) {
				detailSpoken = true;
				speakModName(stack);
			}
		}
	}

	private long estimateSpeechDuration(String text, double rateMultiplier) {
		int cjkCount = 0;
		int wordCount = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
					|| Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL
					|| Character.UnicodeScript.of(c) == Character.UnicodeScript.HIRAGANA
					|| Character.UnicodeScript.of(c) == Character.UnicodeScript.KATAKANA) {
				cjkCount++;
			} else if (Character.isWhitespace(c) || i == text.length() - 1) {
				wordCount++;
			}
		}
		if (wordCount == 0 && cjkCount == 0) wordCount = 1;
		long ms = (long) ((cjkCount * 130 + wordCount * 80) * rateMultiplier);
		return Math.max(200, Math.min(ms, 5000));
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
		hoverStartedAtMs = 0L; nameSpokenAtMs = 0L; estimatedSpeechEndMs = 0L;
		spokenForCurrentHover = false; detailSpoken = false;
	}

	public Optional<String> currentItemName() {
		return currentItemName.isBlank() ? Optional.empty() : Optional.of(currentItemName);
	}
}