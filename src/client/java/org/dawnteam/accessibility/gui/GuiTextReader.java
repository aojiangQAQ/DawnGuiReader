package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.Screen;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.List;

public final class GuiTextReader {
	private static final long RESCAN_INTERVAL_MS = 100L;

	private Screen lastScreen;
	private String lastWidgetText = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;
	private double lastMouseX = -1, lastMouseY = -1;
	private String cachedText = null;
	private long lastScanAtMs;

	public void update(Minecraft client, Screen screen) {
		if (!DawnAccessibilityClient.config().isGuiTextReaderEnabled() || !DawnAccessibilityClient.config().isEnabled()) {
			if (lastScreen != null) reset();
			return;
		}

		if (screen != lastScreen) {
			lastScreen = screen;
			lastWidgetText = "";
			hoverStartedAtMs = 0;
			spokenForCurrent = false;
			cachedText = null;
			lastMouseX = -1; lastMouseY = -1;
			lastScanAtMs = 0L;
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
		long now = System.currentTimeMillis();

		String foundText;
		boolean mouseStable = Math.abs(mouseX - lastMouseX) < 2 && Math.abs(mouseY - lastMouseY) < 2;
		if (mouseStable && now - lastScanAtMs < RESCAN_INTERVAL_MS) {
			foundText = cachedText;
		} else {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
			foundText = findWidgetText(screen.children(), mouseX, mouseY);
			cachedText = foundText;
			lastScanAtMs = now;
		}

		if (foundText == null) {
			if (!lastWidgetText.isEmpty()) { lastWidgetText = ""; spokenForCurrent = false; }
			return;
		}

		if (!foundText.equals(lastWidgetText)) {
			lastWidgetText = foundText;
			hoverStartedAtMs = now;
			spokenForCurrent = false;
		}

		int delay = DawnAccessibilityClient.config().getGuiTextDelayMs();
		if (!spokenForCurrent && now - hoverStartedAtMs >= delay) {
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
			if (child instanceof WorldSelectionList.WorldListEntry worldEntry
					&& worldEntry.isMouseOver(mouseX, mouseY)) {
				String name = worldEntry.getLevelName();
				if (name != null && !name.isBlank()) return name;
			}
			if (child instanceof ServerSelectionList.OnlineServerEntry serverEntry
					&& serverEntry.isMouseOver(mouseX, mouseY)) {
				var data = serverEntry.getServerData();
				if (data != null) {
					String name = data.name;
					if (name != null && !name.isBlank()) return name;
				}
			}
			if (child instanceof ContainerEventHandler container) {
				String found = findWidgetText(container.children(), mouseX, mouseY);
				if (found != null) return found;
			}
		}
		return null;
	}

	private void reset() {
		lastScreen = null;
		lastWidgetText = "";
		hoverStartedAtMs = 0L;
		spokenForCurrent = false;
		lastMouseX = -1;
		lastMouseY = -1;
		cachedText = null;
		lastScanAtMs = 0L;
	}
}
