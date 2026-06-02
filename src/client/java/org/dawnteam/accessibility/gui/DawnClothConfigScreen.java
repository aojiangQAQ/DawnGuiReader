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
				.setDefaultValue(false)
				.setSaveConsumer(config::setEnabled)
				.build());
		general.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.rate_label"),
						config.getSpeechRate(), -10, 10)
				.setDefaultValue(0)
				.setSaveConsumer(config::setSpeechRate)
				.build());
		general.addEntry(entry.startIntSlider(
						Component.translatable("screen.dawn_accessibility.volume_label"),
						config.getVolume(), 0, 100)
				.setDefaultValue(100)
				.setSaveConsumer(config::setVolume)
				.build());
		general.addEntry(entry.startEnumSelector(
						Component.translatable("screen.dawn_accessibility.voice_label"),
						VoiceOption.class, VoiceOption.DEFAULT)
				.setDefaultValue(VoiceOption.DEFAULT)
				.setSaveConsumer(v -> { config.setVoiceId(v == VoiceOption.DEFAULT ? "" : v.name()); config.save(); })
				.build());
		// About
		general.addEntry(entry.startTextDescription(
				Component.translatable("screen.dawn_accessibility.about"))
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
		addDelayField(container, entry, "screen.dawn_accessibility.delay_label",
				config.getHoverDelayMs(), 500, 100, 3000, config::setHoverDelayMs);
		container.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.tooltip_detail_label"),
						config.isTooltipDetailEnabled())
				.setDefaultValue(false)
				.setSaveConsumer(config::setTooltipDetailEnabled)
				.build());
		container.addEntry(entry.startEnumSelector(
						Component.translatable("screen.dawn_accessibility.tooltip_mode_label"),
						TooltipDetailMode.class, TooltipDetailMode.fromInt(config.getTooltipDetailMode()))
				.setDefaultValue(TooltipDetailMode.INDEPENDENT)
				.setSaveConsumer(m -> config.setTooltipDetailMode(m.value))
				.build());
		addDelayField(container, entry, "screen.dawn_accessibility.tooltip_delay_label",
				config.getTooltipDetailDelayMs(), 1000, 200, 3000, config::setTooltipDetailDelayMs);

		// === Hotbar ===
		ConfigCategory hotbar = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.hotbar"));
		hotbar.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.hotbar_label"),
						config.isHotbarReaderEnabled())
				.setDefaultValue(true)
				.setSaveConsumer(config::setHotbarReaderEnabled)
				.build());
		addDelayField(hotbar, entry, "screen.dawn_accessibility.hotbar_delay_label",
				config.getHotbarDelayMs(), 500, 100, 3000, config::setHotbarDelayMs);

		// === Crosshair ===
		ConfigCategory crosshair = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.crosshair"));
		crosshair.addEntry(entry.startEnumSelector(
						Component.translatable("screen.dawn_accessibility.crosshair_mode_label"),
						CrosshairMode.class, CrosshairMode.fromInt(config.getCrosshairMode()))
				.setDefaultValue(CrosshairMode.MANUAL)
				.setSaveConsumer(m -> config.setCrosshairMode(m.value))
				.build());
		addDelayField(crosshair, entry, "screen.dawn_accessibility.block_delay_label",
				config.getBlockDelayMs(), 500, 100, 3000, config::setBlockDelayMs);

		// === GUI ===
		ConfigCategory gui = builder.getOrCreateCategory(
				Component.translatable("screen.dawn_accessibility.section.gui"));
		gui.addEntry(entry.startBooleanToggle(
						Component.translatable("screen.dawn_accessibility.gui_text_label"),
						config.isGuiTextReaderEnabled())
				.setDefaultValue(false)
				.setSaveConsumer(config::setGuiTextReaderEnabled)
				.build());
		addDelayField(gui, entry, "screen.dawn_accessibility.gui_delay_label",
				config.getGuiTextDelayMs(), 500, 100, 3000, config::setGuiTextDelayMs);

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

	private static void addDelayField(ConfigCategory category, ConfigEntryBuilder entry,
			String labelKey, int current, int defaultVal, int min, int max,
			java.util.function.Consumer<Integer> save) {
		category.addEntry(entry.startIntField(
						Component.translatable(labelKey), current)
				.setDefaultValue(defaultVal)
				.setMin(min)
				.setMax(max)
				.setSaveConsumer(save)
				.build());
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

	public enum VoiceOption {
		DEFAULT;
		@Override public String toString() { return "默认 (Default)"; }
	}

	public enum TooltipDetailMode {
		INDEPENDENT(0), SEQUENTIAL(1);
		public final int value;
		TooltipDetailMode(int value) { this.value = value; }
		public static TooltipDetailMode fromInt(int v) { return v == 1 ? SEQUENTIAL : INDEPENDENT; }
		@Override public String toString() {
			return switch (this) { case INDEPENDENT -> "独立"; case SEQUENTIAL -> "顺序"; };
		}
	}

	public enum CrosshairMode {
		OFF(0), AUTO(1), MANUAL(2);
		public final int value;
		CrosshairMode(int value) { this.value = value; }
		public static CrosshairMode fromInt(int v) { return switch (v) { case 0 -> OFF; case 1 -> AUTO; default -> MANUAL; }; }
		@Override public String toString() { return switch (this) { case OFF -> "OFF"; case AUTO -> "AUTO"; case MANUAL -> "MANUAL"; }; }
	}
}
