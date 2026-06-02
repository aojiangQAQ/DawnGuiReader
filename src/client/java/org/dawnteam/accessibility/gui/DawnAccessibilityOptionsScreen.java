package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.dawnteam.accessibility.config.DawnAccessibilityConfig;

public final class DawnAccessibilityOptionsScreen extends Screen {
	private final Screen parent;
	private final DawnAccessibilityConfig config;
	private int scrollOffset = 0;
	private static final int ROW_H = 24;
	private static final int TITLE_AREA = 56;

	public DawnAccessibilityOptionsScreen(Screen parent) {
		super(Component.translatable("screen.dawn_accessibility.title"));
		this.parent = parent;
		this.config = DawnAccessibilityClient.config();
	}

	@Override
	protected void init() {
		int cx = width / 2;
		int y = TITLE_AREA - scrollOffset;
		int hw = 150, gap = 5, fw = hw * 2 + gap;

		addRow(y, hw, gap,
			Button.builder(enabledText(), b -> { config.setEnabled(!config.isEnabled()); config.save(); b.setMessage(enabledText()); }),
			Button.builder(largeText(), b -> { config.setLargeTextEnabled(!config.isLargeTextEnabled()); config.save(); b.setMessage(largeText()); }));

		y += ROW_H;
		addRenderableWidget(new IntSlider(cx - hw, y, fw, 20, -10, 10, config.getSpeechRate(),
				v -> Component.translatable("screen.dawn_accessibility.rate", v),
				v -> { config.setSpeechRate(v); config.save(); }));

		y += ROW_H;
		addDelayRow(y, cx, hw, 100, 3000, config.getHoverDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.delay", v),
				v -> { config.setHoverDelayMs(v); config.save(); });

		y += ROW_H;
		addRenderableWidget(new IntSlider(cx - hw, y, fw, 20, 24, 96, config.getLargeTextSize(),
				v -> Component.translatable("screen.dawn_accessibility.size", v),
				v -> { config.setLargeTextSize(v); config.save(); }));

		y += ROW_H;
		addRenderableWidget(Button.builder(
				Component.translatable("screen.dawn_accessibility.voice", "Default"),
				b -> {
					if (Minecraft.getInstance().player != null) {
						Minecraft.getInstance().player.sendOverlayMessage(
								Component.translatable("message.dawn_accessibility.voice_coming_soon"));
					}
				}).bounds(cx - hw, y, fw, 20).build());

		y += ROW_H + 4;
		addRow(y, hw, gap,
			Button.builder(hotbarText(), b -> { config.setHotbarReaderEnabled(!config.isHotbarReaderEnabled()); config.save(); b.setMessage(hotbarText()); }),
			Button.builder(crosshairText(), b -> { config.setCrosshairMode((config.getCrosshairMode() + 1) % 3); config.save(); b.setMessage(crosshairText()); }));

		y += ROW_H;
		addDelayRow(y, cx, hw, 100, 3000, config.getHotbarDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.hotbar_delay", v),
				v -> { config.setHotbarDelayMs(v); config.save(); });

		y += ROW_H;
		addDelayRow(y, cx, hw, 100, 3000, config.getBlockDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.block_delay", v),
				v -> { config.setBlockDelayMs(v); config.save(); });

		y += ROW_H + 8;
		addRenderableWidget(Button.builder(Component.translatable("screen.dawn_accessibility.back"),
				b -> Minecraft.getInstance().setScreen(parent)).bounds(cx - 100, y, 200, 20).build());
	}

	private void addRow(int y, int hw, int gap, Button.Builder left, Button.Builder right) {
		int cx = width / 2;
		addRenderableWidget(left.bounds(cx - hw, y, hw, 20).build());
		addRenderableWidget(right.bounds(cx + gap, y, hw, 20).build());
	}

	private void addDelayRow(int y, int cx, int hw, int min, int max, int init, LabelFactory label, ValueConsumer onChange) {
		int sliderW = hw * 2 - 110;
		IntSlider slider = new IntSlider(cx - hw, y, sliderW, 20, min, max, init, label, onChange);

		EditBox editBox = new EditBox(font, cx - hw + sliderW + 5, y, 105, 20, Component.empty());
		editBox.setValue(String.valueOf(init));
		editBox.setMaxLength(4);
		editBox.setResponder(text -> {
			if (!text.isEmpty()) {
				try {
					int val = Math.max(min, Math.min(max, Integer.parseInt(text)));
					onChange.accept(val);
					slider.setIntValue(val);
				} catch (NumberFormatException ignored) {}
			}
		});

		slider.linkEditBox(editBox);
		addRenderableWidget(slider);
		addRenderableWidget(editBox);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int contentBottom = TITLE_AREA + 9 * ROW_H + 28;
		int maxScroll = Math.max(0, contentBottom - height + 10);
		int old = scrollOffset;
		scrollOffset = Math.max(0, Math.min(scrollOffset - (int)(verticalAmount * 20), maxScroll));
		if (scrollOffset != old) { clearWidgets(); init(); }
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

	private static final class IntSlider extends AbstractSliderButton {
		private final int min, max;
		private final LabelFactory label;
		private final ValueConsumer consumer;
		private EditBox linkedEditBox;

		private IntSlider(int x, int y, int w, int h, int min, int max, int init, LabelFactory l, ValueConsumer c) {
			super(x, y, w, h, l.create(init), max <= min ? 0 : (double)(init - min) / (max - min));
			this.min = min; this.max = max; this.label = l; this.consumer = c; updateMessage();
		}

		public void linkEditBox(EditBox eb) { this.linkedEditBox = eb; }

		public void setIntValue(int v) {
			this.value = (double)(Math.max(min, Math.min(max, v)) - min) / (max - min);
			updateMessage();
		}

		@Override protected void updateMessage() { setMessage(label.create(current())); }
		@Override protected void applyValue() {
			consumer.accept(current());
			if (linkedEditBox != null) linkedEditBox.setValue(String.valueOf(current()));
		}
		private int current() { return min + (int) Math.round(value * (max - min)); }
	}

	@FunctionalInterface private interface LabelFactory { Component create(int v); }
	@FunctionalInterface private interface ValueConsumer { void accept(int v); }
}
