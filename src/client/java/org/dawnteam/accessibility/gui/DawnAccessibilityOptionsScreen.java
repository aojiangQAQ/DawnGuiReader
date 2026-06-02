package org.dawnteam.accessibility.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.dawnteam.accessibility.config.DawnAccessibilityConfig;
import org.lwjgl.glfw.GLFW;

public final class DawnAccessibilityOptionsScreen extends Screen {
	private final Screen parent;
	private final DawnAccessibilityConfig config;
	private int scrollOffset = 0;
	private KeyMapping activeKeybind;
	private Button activeKeybindButton;
	private static final int ROW_H = 24;
	private static final int SEC_GAP = 8;
	private static final int TITLE_H = 50;

	public DawnAccessibilityOptionsScreen(Screen parent) {
		super(Component.translatable("screen.dawn_accessibility.title"));
		this.parent = parent;
		this.config = DawnAccessibilityClient.config();
	}

	@Override
	protected void init() {
		int cx = width / 2, hw = 150, gap = 5, fw = hw * 2 + gap;
		int y = TITLE_H - scrollOffset;

		// === General ===
		y = addSection(y, "screen.dawn_accessibility.section.general");
		addRow(y, hw, gap,
			Button.builder(onOff("screen.dawn_accessibility.enabled", config.isEnabled()), b -> {
				config.setEnabled(!config.isEnabled()); config.save();
				b.setMessage(onOff("screen.dawn_accessibility.enabled", config.isEnabled()));
			}),
			Button.builder(Component.translatable("screen.dawn_accessibility.voice", "Default"), b -> {
				if (Minecraft.getInstance().player != null)
					Minecraft.getInstance().player.sendOverlayMessage(Component.translatable("message.dawn_accessibility.voice_coming_soon"));
			}));
		y += ROW_H;
		addSlider(y, cx, hw, fw, -10, 10, config.getSpeechRate(),
				v -> Component.translatable("screen.dawn_accessibility.rate", v),
				v -> { config.setSpeechRate(v); config.save(); });

		// === Container ===
		y += ROW_H + SEC_GAP;
		y = addSection(y, "screen.dawn_accessibility.section.container");
		addDelayRow(y, cx, hw, 100, 3000, config.getHoverDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.delay", v),
				v -> { config.setHoverDelayMs(v); config.save(); });
		y += ROW_H;
		addRow(y, hw, gap,
			onOffBtn("screen.dawn_accessibility.tooltip_detail", config.isTooltipDetailEnabled(),
				v -> { config.setTooltipDetailEnabled(v); config.save(); }),
			null);
		addDelayRow(y + ROW_H, cx, hw, 200, 3000, config.getTooltipDetailDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.tooltip_delay", v),
				v -> { config.setTooltipDetailDelayMs(v); config.save(); });

		// === Hotbar ===
		y += ROW_H * 2 + SEC_GAP;
		y = addSection(y, "screen.dawn_accessibility.section.hotbar");
		addRow(y, hw, gap,
			onOffBtn("screen.dawn_accessibility.hotbar", config.isHotbarReaderEnabled(),
				v -> { config.setHotbarReaderEnabled(v); config.save(); }),
			null);
		addDelayRow(y + ROW_H, cx, hw, 100, 3000, config.getHotbarDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.hotbar_delay", v),
				v -> { config.setHotbarDelayMs(v); config.save(); });

		// === Crosshair ===
		y += ROW_H * 2 + SEC_GAP;
		y = addSection(y, "screen.dawn_accessibility.section.crosshair");
		addRow(y, hw, gap,
			Button.builder(crosshairText(), b -> {
				config.setCrosshairMode((config.getCrosshairMode() + 1) % 3); config.save();
				b.setMessage(crosshairText());
			}),
			null);
		addDelayRow(y + ROW_H, cx, hw, 100, 3000, config.getBlockDelayMs(),
				v -> Component.translatable("screen.dawn_accessibility.block_delay", v),
				v -> { config.setBlockDelayMs(v); config.save(); });

		// === GUI ===
		y += ROW_H * 2 + SEC_GAP;
		y = addSection(y, "screen.dawn_accessibility.section.gui");
		addRow(y, hw, gap,
			onOffBtn("screen.dawn_accessibility.gui_text", config.isGuiTextReaderEnabled(),
				v -> { config.setGuiTextReaderEnabled(v); config.save(); }),
			null);

		// === Keybindings ===
		y += ROW_H + SEC_GAP;
		y = addSection(y, "screen.dawn_accessibility.section.keys");
		y = addKeybindRow(y, cx, hw, "screen.dawn_accessibility.keybind.toggle", DawnAccessibilityClient.toggleReaderKey());
		y = addKeybindRow(y, cx, hw, "screen.dawn_accessibility.keybind.repeat", DawnAccessibilityClient.repeatItemKey());
		y = addKeybindRow(y, cx, hw, "screen.dawn_accessibility.keybind.crosshair", DawnAccessibilityClient.crosshairReadKey());

		// Done button
		y += SEC_GAP;
		addRenderableWidget(Button.builder(Component.translatable("screen.dawn_accessibility.back"),
				b -> Minecraft.getInstance().setScreen(parent)).bounds(cx - 100, y, 200, 20).build());
	}

	private int addSection(int y, String key) {
		Button btn = Button.builder(Component.translatable(key), b -> {}).bounds(width / 2 - 155, y, 310, 16).build();
		btn.active = false;
		addRenderableWidget(btn);
		return y + 18;
	}

	private int addKeybindRow(int y, int cx, int hw, String labelKey, KeyMapping mapping) {
		addRenderableWidget(Button.builder(Component.translatable(labelKey), b -> {}).bounds(cx - hw, y, hw, 20).build());
		Button keyBtn = Button.builder(keybindLabel(mapping), b -> {
			cancelCurrentKeybind();
			activeKeybind = mapping;
			activeKeybindButton = b;
			b.setMessage(Component.translatable("screen.dawn_accessibility.press_key"));
		}).bounds(cx + 5, y, hw, 20).build();
		addRenderableWidget(keyBtn);
		return y + ROW_H;
	}

	private void cancelCurrentKeybind() {
		if (activeKeybind != null && activeKeybindButton != null) {
			activeKeybindButton.setMessage(keybindLabel(activeKeybind));
		}
		activeKeybind = null;
		activeKeybindButton = null;
	}

	private void addRow(int y, int hw, int gap, Button.Builder left, Button.Builder right) {
		int cx = width / 2;
		addRenderableWidget(left.bounds(cx - hw, y, hw, 20).build());
		if (right != null) addRenderableWidget(right.bounds(cx + gap, y, hw, 20).build());
	}

	private void addSlider(int y, int cx, int hw, int fw, int min, int max, int init, LabelFactory l, ValueConsumer c) {
		addRenderableWidget(new IntSlider(cx - hw, y, fw, 20, min, max, init, l, c));
	}

	private void addDelayRow(int y, int cx, int hw, int min, int max, int init, LabelFactory l, ValueConsumer c) {
		int sliderW = hw * 2 - 110;
		IntSlider slider = new IntSlider(cx - hw, y, sliderW, 20, min, max, init, l, c);
		EditBox eb = new EditBox(font, cx - hw + sliderW + 5, y, 105, 20, Component.empty());
		eb.setValue(String.valueOf(init));
		eb.setMaxLength(4);
		eb.setResponder(text -> {
			if (!text.isEmpty()) {
				try { int v = Math.max(min, Math.min(max, Integer.parseInt(text))); c.accept(v); slider.setIntValue(v); }
				catch (NumberFormatException ignored) {}
			}
		});
		slider.linkEditBox(eb);
		addRenderableWidget(slider);
		addRenderableWidget(eb);
	}

	private Button.Builder onOffBtn(String key, boolean on, java.util.function.Consumer<Boolean> toggle) {
		return Button.builder(onOff(key, on), b -> { toggle.accept(!on); b.setMessage(onOff(key, !on)); });
	}
	private Component onOff(String key, boolean on) { return Component.translatable(key, on ? "ON" : "OFF"); }
	private Component crosshairText() {
		String m = switch (config.getCrosshairMode()) { case 0 -> "OFF"; case 1 -> "AUTO"; default -> "MANUAL"; };
		return Component.translatable("screen.dawn_accessibility.crosshair_mode", m);
	}
	private static Component keybindLabel(KeyMapping m) {
		return Component.translatable("screen.dawn_accessibility.keybind_value", m.getTranslatedKeyMessage().getString());
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double ha, double va) {
		int max = Math.max(0, TITLE_H + 22 * ROW_H - height + 20);
		int old = scrollOffset;
		scrollOffset = Math.max(0, Math.min(scrollOffset - (int)(va * 20), max));
		if (scrollOffset != old) { clearWidgets(); init(); }
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (activeKeybind != null) {
			int keyCode = event.input();
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				cancelCurrentKeybind();
			} else {
				activeKeybind.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
				if (activeKeybindButton != null) {
					activeKeybindButton.setMessage(keybindLabel(activeKeybind));
				}
				activeKeybind = null;
				activeKeybindButton = null;
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
		super.extractRenderState(g, mx, my, pt);
		g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
		Component st = Component.translatable("screen.dawn_accessibility.status",
				Component.translatable(DawnAccessibilityClient.ttsEngine().isAvailable()
						? "screen.dawn_accessibility.available" : "screen.dawn_accessibility.unavailable"));
		g.centeredText(font, st, width / 2, 26, 0xFFCCCCCC);
	}

	// --- IntSlider ---
	private static final class IntSlider extends AbstractSliderButton {
		private final int min, max;
		private final LabelFactory label;
		private final ValueConsumer consumer;
		private EditBox linkedEditBox;
		private IntSlider(int x, int y, int w, int h, int min, int max, int init, LabelFactory l, ValueConsumer c) {
			super(x, y, w, h, l.create(init), max <= min ? 0 : (double)(init - min) / (max - min));
			this.min = min; this.max = max; this.label = l; this.consumer = c; updateMessage();
		}
		public void linkEditBox(EditBox eb) { linkedEditBox = eb; }
		public void setIntValue(int v) { value = (double)(Math.max(min, Math.min(max, v)) - min) / (max - min); updateMessage(); }
		@Override protected void updateMessage() { setMessage(label.create(current())); }
		@Override protected void applyValue() { consumer.accept(current()); if (linkedEditBox != null) linkedEditBox.setValue(String.valueOf(current())); }
		private int current() { return min + (int) Math.round(value * (max - min)); }
	}

	@FunctionalInterface private interface LabelFactory { Component create(int v); }
	@FunctionalInterface private interface ValueConsumer { void accept(int v); }
}