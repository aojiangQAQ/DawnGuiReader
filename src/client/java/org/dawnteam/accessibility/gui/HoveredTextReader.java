package org.dawnteam.accessibility.gui;

import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class HoveredTextReader {
	private String lastText = "";
	private String currentText = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrentHover;

	public void update(String text) {
		if (text == null || text.isBlank()) {
			reset();
			return;
		}

		if (!text.equals(lastText)) {
			lastText = text;
			currentText = text;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrentHover = false;
		}

		if (!spokenForCurrentHover
				&& DawnAccessibilityClient.config().isEnabled()
				&& System.currentTimeMillis() - hoverStartedAtMs >= DawnAccessibilityClient.config().getHoverDelayMs()) {
			spokenForCurrentHover = true;
			DawnAccessibilityClient.speak(text);
		}
	}

	public void reset() {
		lastText = "";
		currentText = "";
		hoverStartedAtMs = 0L;
		spokenForCurrentHover = false;
	}

	public Optional<String> currentText() {
		return currentText.isBlank() ? Optional.empty() : Optional.of(currentText);
	}
}
