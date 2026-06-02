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
	private boolean containerReaderEnabled = true;
	private int hoverDelayMs = 500;
	private int speechRate = 0;
	private String voiceId = "";
	private boolean hotbarReaderEnabled = true;
	private int hotbarDelayMs = 500;
	private int crosshairMode = 2;
	private int blockDelayMs = 500;
	private boolean tooltipDetailEnabled = false;
	private int tooltipDetailDelayMs = 1000;
	private boolean guiTextReaderEnabled = false;
	private int guiTextDelayMs = 500;

	public static DawnAccessibilityConfig load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				DawnAccessibilityConfig loaded = GSON.fromJson(reader, DawnAccessibilityConfig.class);
				if (loaded != null) { loaded.clamp(); return loaded; }
			} catch (IOException | RuntimeException e) {
				DawnAccessibilityClient.LOGGER.warn("Failed to load config", e);
			}
		}
		DawnAccessibilityConfig c = new DawnAccessibilityConfig();
		c.save();
		return c;
	}

	public void save() {
		clamp();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) { GSON.toJson(this, w); }
		} catch (IOException e) { DawnAccessibilityClient.LOGGER.warn("Failed to save config", e); }
	}

	private void clamp() {
		hoverDelayMs = clamp(hoverDelayMs, 100, 3000);
		speechRate = clamp(speechRate, -10, 10);
		hotbarDelayMs = clamp(hotbarDelayMs, 100, 3000);
		blockDelayMs = clamp(blockDelayMs, 100, 3000);
		tooltipDetailDelayMs = clamp(tooltipDetailDelayMs, 200, 3000);
		guiTextDelayMs = clamp(guiTextDelayMs, 100, 3000);
		crosshairMode = clamp(crosshairMode, 0, 2);
		if (voiceId == null) voiceId = "";
	}
	private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean v) { enabled = v; }
	public boolean isContainerReaderEnabled() { return containerReaderEnabled; }
	public void setContainerReaderEnabled(boolean v) { containerReaderEnabled = v; }
	public int getHoverDelayMs() { return hoverDelayMs; }
	public void setHoverDelayMs(int v) { hoverDelayMs = v; clamp(); }
	public int getSpeechRate() { return speechRate; }
	public void setSpeechRate(int v) { speechRate = v; clamp(); }
	public String getVoiceId() { return voiceId; }
	public void setVoiceId(String v) { voiceId = v == null ? "" : v; }
	public boolean isHotbarReaderEnabled() { return hotbarReaderEnabled; }
	public void setHotbarReaderEnabled(boolean v) { hotbarReaderEnabled = v; }
	public int getHotbarDelayMs() { return hotbarDelayMs; }
	public void setHotbarDelayMs(int v) { hotbarDelayMs = v; clamp(); }
	public int getCrosshairMode() { return crosshairMode; }
	public void setCrosshairMode(int v) { crosshairMode = v; clamp(); }
	public int getBlockDelayMs() { return blockDelayMs; }
	public void setBlockDelayMs(int v) { blockDelayMs = v; clamp(); }
	public boolean isTooltipDetailEnabled() { return tooltipDetailEnabled; }
	public void setTooltipDetailEnabled(boolean v) { tooltipDetailEnabled = v; }
	public int getTooltipDetailDelayMs() { return tooltipDetailDelayMs; }
	public void setTooltipDetailDelayMs(int v) { tooltipDetailDelayMs = v; clamp(); }
	public boolean isGuiTextReaderEnabled() { return guiTextReaderEnabled; }
	public void setGuiTextReaderEnabled(boolean v) { guiTextReaderEnabled = v; }
	public int getGuiTextDelayMs() { return guiTextDelayMs; }
	public void setGuiTextDelayMs(int v) { guiTextDelayMs = v; clamp(); }
}