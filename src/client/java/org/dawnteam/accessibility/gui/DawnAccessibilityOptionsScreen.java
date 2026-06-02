package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.dawnteam.accessibility.config.DawnAccessibilityConfig;
import org.dawnteam.accessibility.tts.Voice;

import java.util.List;

public final class DawnAccessibilityOptionsScreen extends Screen {
	private final Screen parent;
	private final DawnAccessibilityConfig config;
	private List<Voice> voices;
	private int voiceIndex;
	private int scrollOffset = 0;
	private static final int ROW_HEIGHT = 24;
	private static final int TITLE_AREA = 56;

	public DawnAccessibilityOptionsScreen(Screen parent) {
		super(Component.translatable("screen.dawn_accessibility.title"));
		this.parent = parent;
		this.config = DawnAccessibilityClient.config();
	}

	@Override
	protected void init() {
		voices = DawnAccessibilityClient.ttsEngine().listVoices();
		voiceIndex = currentVoiceIndex();

		int centerX = width / 2;
		int y = TITLE_AREA - scrollOffset;
		int halfW = 150;
		int gap = 5;
		int fullW = halfW * 2 + gap;

		addRow(y, halfW, gap,
			Button.builder(enabledText(), b -> { config.setEnabled(!config.isEnabled()); config.save(); b.setMessage(enabledText()); }),
			Button.builder(largeText(), b -> { config.setLargeTextEnabled(!config.isLargeTextEnabled()); config.save(); b.setMessage(largeText()); })
		);

		y += ROW_HEIGHT;
		addRenderableWidget(new IntSlider(centerX - halfW, y, fullW, 20, -10, 10, config.getSpeechRate(),
				v -> Component.translatable("screen.dawn_accessibility.rate", v),
				v -> { config.setSpeechRate(v); config.save(); }));

		y += ROW_HEIGHT;
		addRenderableWidget(new IntSlider(centerX - halfW, y, fullW, 20, 100, 3000, config.getHoverDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.delay", v),
				v -> { config.setHoverDelayMs(v); config.save(); }));

		y += ROW_HEIGHT;
		addRenderableWidget(new IntSlider(centerX - halfW, y, fullW, 20, 24, 96, config.getLargeTextSize(),
				v -> Component.translatable("screen.dawn_accessibility.size", v),
				v -> { config.setLargeTextSize(v); config.save(); }));

		y += ROW_HEIGHT;
		addRenderableWidget(Button.builder(voiceText(), b -> {
			if (!voices.isEmpty()) {
				voiceIndex = (voiceIndex + 1) % voices.size();
				config.setVoiceId(voices.get(voiceIndex).id());
				config.save();
				b.setMessage(voiceText());
			}
		}).bounds(centerX - halfW, y, fullW, 20).build());

		y += ROW_HEIGHT + 4;
		addRow(y, halfW, gap,
			Button.builder(hotbarText(), b -> { config.setHotbarReaderEnabled(!config.isHotbarReaderEnabled()); config.save(); b.setMessage(hotbarText()); }),
			Button.builder(crosshairText(), b -> { config.setCrosshairMode((config.getCrosshairMode() + 1) % 3); config.save(); b.setMessage(crosshairText()); })
		);

		y += ROW_HEIGHT;
		addRenderableWidget(new IntSlider(centerX - halfW, y, fullW, 20, 100, 3000, config.getHotbarDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.hotbar_delay", v),
				v -> { config.setHotbarDelayMs(v); config.save(); }));

		y += ROW_HEIGHT;
		addRenderableWidget(new IntSlider(centerX - halfW, y, fullW, 20, 100, 3000, config.getBlockDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.block_delay", v),
				v -> { config.setBlockDelayMs(v); config.save(); }));

		y += ROW_HEIGHT + 8;
		addRenderableWidget(Button.builder(Component.translatable("screen.dawn_accessibility.back"),
				b -> Minecraft.getInstance().setScreen(parent))
				.bounds(centerX - 100, y, 200, 20).build());
	}

	private void addRow(int y, int halfW, int gap, Button.Builder left, Button.Builder right) {
		int centerX = width / 2;
		addRenderableWidget(left.bounds(centerX - halfW, y, halfW, 20).build());
		addRenderableWidget(right.bounds(centerX + gap, y, halfW, 20).build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int contentBottom = TITLE_AREA + 9 * ROW_HEIGHT + 28;
		int maxScroll = Math.max(0, contentBottom - height + 10);
		int old = scrollOffset;
		scrollOffset = Math.max(0, Math.min(scrollOffset - (int)(verticalAmount * 20), maxScroll));
		if (scrollOffset != old) {
			clearWidgets();
			init();
		}
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		Component status = Component.translatable("screen.dawn_accessibility.status",
				Component.translatable(DawnAccessibilityClient.ttsEngine().isAvailable()
						? "screen.dawn_accessibility.available" : "screen.dawn_accessibility.unavailable"));
		graphics.centeredText(font, status, width / 2, 30, 0xFFCCCCCC);
	}

	private Component enabledText() { return Component.translatable("screen.dawn_accessibility.enabled", config.isEnabled() ? "ON" : "OFF"); }
	private Component largeText() { return Component.translatable("screen.dawn_accessibility.large_text", config.isLargeTextEnabled() ? "ON" : "OFF"); }
	private Component hotbarText() { return Component.translatable("screen.dawn_accessibility.hotbar", config.isHotbarReaderEnabled() ? "ON" : "OFF"); }
	private Component crosshairText() {
		String m = switch (config.getCrosshairMode()) { case 0 -> "OFF"; case 1 -> "AUTO"; default -> "MANUAL"; };
		return Component.translatable("screen.dawn_accessibility.crosshair_mode", m);
	}
	private Component voiceText() {
		String v = voices.isEmpty() ? "Default" : voices.get(voiceIndex).displayName();
		return Component.translatable("screen.dawn_accessibility.voice", v);
	}
	private int currentVoiceIndex() {
		for (int i = 0; i < voices.size(); i++) { if (voices.get(i).id().equals(config.getVoiceId())) return i; }
		return 0;
	}

	private static final class IntSlider extends AbstractSliderButton {
		private final int min, max;
		private final LabelFactory label;
		private final ValueConsumer consumer;
		private IntSlider(int x, int y, int w, int h, int min, int max, int init, LabelFactory l, ValueConsumer c) {
			super(x, y, w, h, l.create(init), max <= min ? 0 : (double)(init - min) / (max - min));
			this.min = min; this.max = max; this.label = l; this.consumer = c; updateMessage();
		}
		@Override protected void updateMessage() { setMessage(label.create(current())); }
		@Override protected void applyValue() { consumer.accept(current()); }
		private int current() { return min + (int) Math.round(value * (max - min)); }
	}
	@FunctionalInterface private interface LabelFactory { Component create(int v); }
	@FunctionalInterface private interface ValueConsumer { void accept(int v); }
}
