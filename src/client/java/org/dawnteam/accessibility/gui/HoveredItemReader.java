package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
				speakDetail(stack);
			}
		}
	}

	private long estimateSpeechDuration(String text, double rateMultiplier) {
		int cjkCount = 0;
		int latinCount = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
					|| Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL
					|| Character.UnicodeScript.of(c) == Character.UnicodeScript.HIRAGANA
					|| Character.UnicodeScript.of(c) == Character.UnicodeScript.KATAKANA) {
				cjkCount++;
			} else if (!Character.isWhitespace(c)) {
				latinCount++;
			}
		}
		// Base overhead for TTS startup
		long baseMs = 300;
		// CJK ~180ms/char, Latin ~60ms/char at normal speed
		long contentMs = (long) ((cjkCount * 180 + latinCount * 60) * rateMultiplier);
		return Math.max(400, Math.min(baseMs + contentMs, 6000));
	}

	private void speakDetail(ItemStack stack) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) return;

		StringBuilder sb = new StringBuilder();

		// 1. Read enchantments explicitly from DataComponents
		appendEnchantments(sb, stack.get(DataComponents.ENCHANTMENTS));
		appendEnchantments(sb, stack.get(DataComponents.STORED_ENCHANTMENTS));

		// 2. Read tooltip lines (lore, etc.) - skip enchantments since we already read them
		var tooltipContext = net.minecraft.world.item.Item.TooltipContext.of(client.level);
		var flag = new net.minecraft.world.item.TooltipFlag.Default(false, true);
		var lines = stack.getTooltipLines(tooltipContext, client.player, flag);
		for (int i = 1; i < lines.size(); i++) {
			String t = lines.get(i).getString().trim();
			if (!t.isEmpty() && !isEnchantmentLine(t, stack)) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(t);
			}
		}

		// 3. Append mod name
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
			if (sb.length() > 0) sb.append(", ");
			sb.append(modName);
		}

		if (sb.length() > 0) {
			DawnAccessibilityClient.speak(sb.toString());
		}
	}

	private void appendEnchantments(StringBuilder sb, ItemEnchantments enchants) {
		if (enchants == null || enchants.size() == 0) return;
		for (var entry : enchants.entrySet()) {
			var holder = entry.getKey();
			int level = entry.getIntValue();
			Component name = Enchantment.getFullname(holder, level);
			if (name != null) {
				String text = name.getString().trim();
				if (!text.isEmpty()) {
					if (sb.length() > 0) sb.append(", ");
					sb.append(text);
				}
			}
		}
	}

	private boolean isEnchantmentLine(String line, ItemStack stack) {
		// Check if this tooltip line matches any enchantment name to avoid duplication
		ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (matchesEnchantments(line, enchants)) return true;
		if (matchesEnchantments(line, stored)) return true;
		return false;
	}

	private boolean matchesEnchantments(String line, ItemEnchantments enchants) {
		if (enchants == null) return false;
		for (var entry : enchants.entrySet()) {
			Component name = Enchantment.getFullname(entry.getKey(), entry.getIntValue());
			if (name != null && line.equals(name.getString().trim())) return true;
		}
		return false;
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