package org.dawnteam.accessibility.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.dawnteam.accessibility.config.DawnAccessibilityConfig;
import org.dawnteam.accessibility.mixin.KeyMappingAccessor;

public final class DawnClothConfigScreen {

	public static net.minecraft.client.gui.screens.Screen create(net.minecraft.client.gui.screens.Screen parent) {
		DawnAccessibilityConfig config = DawnAccessibilityClient.config();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("screen.dawn_accessibility.title"))
				.setSavingRunnable(config::save)
				.transparentBackground();

		ConfigEntryBuilder entry = builder.entryBuilder();

		// === General ===
		ConfigCategory general = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.general"));
		general.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.enabled_label"),
						config.isEnabled())
				.setDefaultValue(true)
				.setSaveConsumer(config::setEnabled)
				.build());
		general.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.rate_label"),
						config.getSpeechRate(), -10, 10)
				.setDefaultValue(0)
				.setSaveConsumer(config::setSpeechRate)
				.build());

		// === Container ===
		ConfigCategory container = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.container"));
		container.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.container_label"),
						config.isContainerReaderEnabled())
				.setDefaultValue(true)
				.setSaveConsumer(config::setContainerReaderEnabled)
				.build());
		container.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.delay_label"),
						config.getHoverDelayMs(), 100, 3000)
				.setDefaultValue(500)
				.setSaveConsumer(config::setHoverDelayMs)
				.build());
		container.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.tooltip_detail_label"),
						config.isTooltipDetailEnabled())
				.setDefaultValue(false)
				.setSaveConsumer(config::setTooltipDetailEnabled)
				.build());
		container.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.tooltip_delay_label"),
						config.getTooltipDetailDelayMs(), 200, 3000)
				.setDefaultValue(1000)
				.setSaveConsumer(config::setTooltipDetailDelayMs)
				.build());

		// === Hotbar ===
		ConfigCategory hotbar = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.hotbar"));
		hotbar.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.hotbar_label"),
						config.isHotbarReaderEnabled())
				.setDefaultValue(true)
				.setSaveConsumer(config::setHotbarReaderEnabled)
				.build());
		hotbar.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.hotbar_delay_label"),
						config.getHotbarDelayMs(), 100, 3000)
				.setDefaultValue(500)
				.setSaveConsumer(config::setHotbarDelayMs)
				.build());

		// === Crosshair ===
		ConfigCategory crosshair = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.crosshair"));
		crosshair.addEntry(entry.startEnumSelector(
						Component.translatable("screen.dawn_accessibility.crosshair_mode_label"),
						CrosshairMode.class, CrosshairMode.fromInt(config.getCrosshairMode()))
				.setDefaultValue(CrosshairMode.MANUAL)
				.setSaveConsumer(m -> config.setCrosshairMode(m.value))
				.build());
		crosshair.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.block_delay_label"),
						config.getBlockDelayMs(), 100, 3000)
				.setDefaultValue(500)
				.setSaveConsumer(config::setBlockDelayMs)
				.build());

		// === GUI ===
		ConfigCategory gui = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.gui"));
		gui.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.gui_text_label"),
						config.isGuiTextReaderEnabled())
				.setDefaultValue(false)
				.setSaveConsumer(config::setGuiTextReaderEnabled)
				.build());
		gui.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.gui_delay_label"),
						config.getGuiTextDelayMs(), 100, 3000)
				.setDefaultValue(500)
				.setSaveConsumer(config::setGuiTextDelayMs)
				.build());

		// === Keybindings ===
		ConfigCategory keys = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.keys"));
		addKeybindEntry(keys, entry, "screen.dawn_accessibility.keybind.toggle",
				DawnAccessibilityClient.toggleReaderKey());
		addKeybindEntry(keys, entry, "screen.dawn_accessibility.keybind.repeat",
				DawnAccessibilityClient.repeatItemKey());
		addKeybindEntry(keys, entry, "screen.dawn_accessibility.keybind.crosshair",
				DawnAccessibilityClient.crosshairReadKey());

		return builder.build();
	}

	private static void addKeybindEntry(ConfigCategory category, ConfigEntryBuilder entry,
			String labelKey, KeyMapping mapping) {
		InputConstants.Key currentKey = ((KeyMappingAccessor) mapping).dawnAccessibility$getKey();
		category.addEntry(entry.startKeyCodeField(
						Component.translatable(labelKey), currentKey)
				.setDefaultValue(currentKey)
				.setKeySaveConsumer(mapping::setKey)
				.build());
	}

	public enum CrosshairMode {
		OFF(0), AUTO(1), MANUAL(2);

		public final int value;
		CrosshairMode(int value) { this.value = value; }

		public static CrosshairMode fromInt(int v) {
			return switch (v) { case 0 -> OFF; case 1 -> AUTO; default -> MANUAL; };
		}

		@Override
		public String toString() {
			return switch (this) { case OFF -> "OFF"; case AUTO -> "AUTO"; case MANUAL -> "MANUAL"; };
		}
	}
}