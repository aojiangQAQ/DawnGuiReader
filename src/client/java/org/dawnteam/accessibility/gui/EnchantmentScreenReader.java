package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;

/**
 * Reads enchantment options when hovering over the enchanting table GUI.
 * The 3 options are at known positions relative to the GUI left/top.
 */
public final class EnchantmentScreenReader {
	private int lastHoveredIndex = -1;
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;

	/**
	 * Called each tick when the screen is an EnchantmentScreen.
	 * @param screen the enchantment screen
	 * @param mouseX mouse X in GUI-scaled coordinates
	 * @param mouseY mouse Y in GUI-scaled coordinates
	 */
	public void update(EnchantmentScreen screen, double mouseX, double mouseY) {
		var cfg = DawnAccessibilityClient.config();
		if (!cfg.isContainerReaderEnabled() || !cfg.isEnabled()) {
			reset();
			return;
		}

		EnchantmentMenu menu = screen.getMenu();
		int[] clues = menu.enchantClue;
		int[] levels = menu.levelClue;

		// The 3 enchantment options in the GUI
		// Each option: x=60, y=14+19*i, width=108, height=19 (relative to leftPos/topPos)
		int hoveredIdx = -1;
		for (int i = 0; i < 3; i++) {
			if (isHovering(screen, 60, 14 + 19 * i, 108, 19, mouseX, mouseY)) {
				hoveredIdx = i;
				break;
			}
		}

		if (hoveredIdx < 0) {
			if (lastHoveredIndex >= 0) {
				lastHoveredIndex = -1;
				spokenForCurrent = false;
			}
			return;
		}

		if (hoveredIdx != lastHoveredIndex) {
			lastHoveredIndex = hoveredIdx;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrent = false;
		}

		if (!spokenForCurrent && System.currentTimeMillis() - hoverStartedAtMs >= cfg.getHoverDelayMs()) {
			spokenForCurrent = true;
			int clue = clues[hoveredIdx];
			int level = levels[hoveredIdx];
			String name = resolveEnchantmentName(clue, level);
			if (name != null && !name.isEmpty()) {
				DawnAccessibilityClient.speak(name);
			}
		}
	}

	private boolean isHovering(EnchantmentScreen screen, int x, int y, int w, int h, double mouseX, double mouseY) {
		// We need leftPos and topPos; use the accessor mixin
		int leftPos = ((org.dawnteam.accessibility.mixin.AbstractContainerScreenAccessor) screen).dawnAccessibility$getLeftPos();
		int topPos = ((org.dawnteam.accessibility.mixin.AbstractContainerScreenAccessor) screen).dawnAccessibility$getTopPos();
		return mouseX >= leftPos + x && mouseX < leftPos + x + w
				&& mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	private String resolveEnchantmentName(int clue, int level) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return null;
		try {
			var lookup = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			var holder = lookup.get(clue);
			if (holder.isPresent()) {
				Component fullName = Enchantment.getFullname(holder.get(), level);
				if (fullName != null) {
					return fullName.getString().trim();
				}
			}
		} catch (Exception e) {
			DawnAccessibilityClient.LOGGER.debug("Failed to resolve enchantment clue {}", clue, e);
		}
		return null;
	}

	public void reset() {
		lastHoveredIndex = -1;
		hoverStartedAtMs = 0;
		spokenForCurrent = false;
	}
}