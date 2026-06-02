package org.dawnteam.accessibility.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DawnAccessibilityConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("dawn-accessibility.json");

	private boolean enabled = true;
	private int hoverDelayMs = 500;
	private int speechRate = 0;
	private String voiceId = "";
	private boolean largeTextEnabled = true;
	private int largeTextSize = 48;
	private boolean hotbarReaderEnabled = true;
	private int hotbarDelayMs = 500;
	private int crosshairMode = 2;
	private int blockDelayMs = 500;

	public static DawnAccessibilityConfig load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				DawnAccessibilityConfig loaded = GSON.fromJson(reader, DawnAccessibilityConfig.class);
				if (loaded != null) {
					loaded.clamp();
					return loaded;
				}
			} catch (IOException | RuntimeException exception) {
				DawnAccessibilityClient.LOGGER.warn("Failed to load config, using defaults", exception);
			}
		}
		DawnAccessibilityConfig config = new DawnAccessibilityConfig();
		config.save();
		return config;
	}

	public void save() {
		clamp();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			DawnAccessibilityClient.LOGGER.warn("Failed to save config", exception);
		}
	}

	private void clamp() {
		hoverDelayMs = clamp(hoverDelayMs, 100, 3000);
		speechRate = clamp(speechRate, -10, 10);
		largeTextSize = clamp(largeTextSize, 24, 96);
		hotbarDelayMs = clamp(hotbarDelayMs, 100, 3000);
		blockDelayMs = clamp(blockDelayMs, 100, 3000);
		crosshairMode = clamp(crosshairMode, 0, 2);
		if (voiceId == null) voiceId = "";
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean v) { this.enabled = v; }
	public int getHoverDelayMs() { return hoverDelayMs; }
	public void setHoverDelayMs(int v) { this.hoverDelayMs = v; clamp(); }
	public int getSpeechRate() { return speechRate; }
	public void setSpeechRate(int v) { this.speechRate = v; clamp(); }
	public String getVoiceId() { return voiceId; }
	public void setVoiceId(String v) { this.voiceId = v == null ? "" : v; }
	public boolean isLargeTextEnabled() { return largeTextEnabled; }
	public void setLargeTextEnabled(boolean v) { this.largeTextEnabled = v; }
	public int getLargeTextSize() { return largeTextSize; }
	public void setLargeTextSize(int v) { this.largeTextSize = v; clamp(); }
	public boolean isHotbarReaderEnabled() { return hotbarReaderEnabled; }
	public void setHotbarReaderEnabled(boolean v) { this.hotbarReaderEnabled = v; }
	public int getHotbarDelayMs() { return hotbarDelayMs; }
	public void setHotbarDelayMs(int v) { this.hotbarDelayMs = v; clamp(); }
	public int getCrosshairMode() { return crosshairMode; }
	public void setCrosshairMode(int v) { this.crosshairMode = v; clamp(); }
	public int getBlockDelayMs() { return blockDelayMs; }
	public void setBlockDelayMs(int v) { this.blockDelayMs = v; clamp(); }
}
