package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.dawnteam.accessibility.compat.MinecraftScreenCompat;

import java.util.List;

public final class GuiTextReader {
	private Screen lastScreen;
	private String lastWidgetText = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;
	private double lastMouseX = -1, lastMouseY = -1;
	private String cachedText = null;

	public void update(Minecraft client) {
		if (!DawnAccessibilityClient.config().isGuiTextReaderEnabled() || !DawnAccessibilityClient.config().isEnabled()) {
			if (lastScreen != null) { lastScreen = null; cachedText = null; }
			return;
		}

		Screen screen = MinecraftScreenCompat.currentScreen(client);
		if (screen != lastScreen) {
			lastScreen = screen;
			lastWidgetText = "";
			hoverStartedAtMs = 0;
			spokenForCurrent = false;
			cachedText = null;
			lastMouseX = -1; lastMouseY = -1;
			if (screen != null && screen.getTitle() != null) {
				String title = screen.getTitle().getString().trim();
				if (!title.isEmpty()) DawnAccessibilityClient.speak(title);
			}
			return;
		}

		if (screen == null) return;

		var window = client.getWindow();
		double mouseX = client.mouseHandler.xpos() * screen.width / window.getWidth();
		double mouseY = client.mouseHandler.ypos() * screen.height / window.getHeight();

		// Skip re-iteration if mouse hasn't moved significantly
		String foundText;
		if (Math.abs(mouseX - lastMouseX) < 2 && Math.abs(mouseY - lastMouseY) < 2 && cachedText != null) {
			foundText = cachedText;
		} else {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
			foundText = findWidgetText(screen.children(), mouseX, mouseY);
			cachedText = foundText;
		}

		if (foundText == null) {
			if (!lastWidgetText.isEmpty()) { lastWidgetText = ""; spokenForCurrent = false; }
			return;
		}

		if (!foundText.equals(lastWidgetText)) {
			lastWidgetText = foundText;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrent = false;
		}

		int delay = DawnAccessibilityClient.config().getGuiTextDelayMs();
		if (!spokenForCurrent && System.currentTimeMillis() - hoverStartedAtMs >= delay) {
			spokenForCurrent = true;
			DawnAccessibilityClient.speak(foundText);
		}
	}

	private String findWidgetText(List<? extends GuiEventListener> children, double mouseX, double mouseY) {
		for (var child : children) {
			if (child instanceof AbstractWidget widget && widget.visible && widget.isActive()
					&& widget.isMouseOver(mouseX, mouseY)) {
				String text = widget.getMessage().getString().trim();
				if (!text.isEmpty()) return text;
			}
			// Recipe book buttons
			if (child instanceof RecipeButton recipeBtn && recipeBtn.visible && recipeBtn.isActive()
					&& recipeBtn.isMouseOver(mouseX, mouseY)) {
				String text = recipeBtn.getMessage().getString().trim();
				if (!text.isEmpty()) return text;
			}
			// World selection list entries
			if (child instanceof WorldSelectionList.WorldListEntry worldEntry
					&& worldEntry.isMouseOver(mouseX, mouseY)) {
				String name = worldEntry.getLevelName();
				if (name != null && !name.isBlank()) return name;
			}
			// Server selection list entries
			if (child instanceof ServerSelectionList.OnlineServerEntry serverEntry
					&& serverEntry.isMouseOver(mouseX, mouseY)) {
				var data = serverEntry.getServerData();
				if (data != null) {
					String name = data.name;
					if (name != null && !name.isBlank()) return name;
				}
			}
			// Recurse into containers
			if (child instanceof ContainerEventHandler container) {
				String found = findWidgetText(container.children(), mouseX, mouseY);
				if (found != null) return found;
			}
		}
		return null;
	}
}