package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.List;

public final class GuiTextReader {
	private Screen lastScreen;
	private String lastWidgetText = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;

	public void update(Minecraft client) {
		if (!DawnAccessibilityClient.config().isGuiTextReaderEnabled() || !DawnAccessibilityClient.config().isEnabled()) {
			if (lastScreen != null) lastScreen = null;
			return;
		}

		Screen screen = client.screen;
		if (screen != lastScreen) {
			lastScreen = screen;
			lastWidgetText = "";
			hoverStartedAtMs = 0;
			spokenForCurrent = false;
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

		String foundText = findWidgetText(screen.children(), mouseX, mouseY);

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
			// Recurse into containers (OptionsList entries, sub-screens, etc.)
			if (child instanceof ContainerEventHandler container) {
				String found = findWidgetText(container.children(), mouseX, mouseY);
				if (found != null) return found;
			}
		}
		return null;
	}
}