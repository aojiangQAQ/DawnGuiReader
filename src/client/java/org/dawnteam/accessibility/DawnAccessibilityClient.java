package org.dawnteam.accessibility;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.dawnteam.accessibility.config.DawnAccessibilityConfig;
import org.dawnteam.accessibility.compat.MinecraftScreenCompat;
import org.dawnteam.accessibility.gui.BlockTargetReader;
import org.dawnteam.accessibility.gui.EnchantmentScreenReader;
import org.dawnteam.accessibility.gui.GuiTextReader;
import org.dawnteam.accessibility.gui.HotbarItemReader;
import org.dawnteam.accessibility.gui.HoveredItemReader;
import org.dawnteam.accessibility.gui.HoveredTextReader;
import org.dawnteam.accessibility.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.dawnteam.accessibility.tts.SystemTtsEngine;
import org.dawnteam.accessibility.tts.TtsEngine;
import org.dawnteam.accessibility.tts.TtsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DawnAccessibilityClient implements ClientModInitializer {
	public static final String MOD_ID = "dawn_accessibility";
	public static final Logger LOGGER = LoggerFactory.getLogger("Dawn GUI Reader");

	private static DawnAccessibilityConfig config;
	private static TtsEngine ttsEngine;
	private static HoveredItemReader hoveredItemReader;
	private static HoveredTextReader hoveredCreativeTabReader;
	private static HotbarItemReader hotbarItemReader;
	private static BlockTargetReader blockTargetReader;
	private static GuiTextReader guiTextReader;
	private static EnchantmentScreenReader enchantmentScreenReader;

	private static KeyMapping toggleReaderKey;
	private static KeyMapping repeatItemKey;
	private static KeyMapping crosshairReadKey;
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "accessibility"));

	private static int toggleClicks, repeatClicks, crosshairClicks;

	@Override
	public void onInitializeClient() {
		config = DawnAccessibilityConfig.load();
		ttsEngine = new SystemTtsEngine(LOGGER);
		hoveredItemReader = new HoveredItemReader();
		hoveredCreativeTabReader = new HoveredTextReader();
		hotbarItemReader = new HotbarItemReader();
		blockTargetReader = new BlockTargetReader();
		guiTextReader = new GuiTextReader();
		enchantmentScreenReader = new EnchantmentScreenReader();

		registerKeyBindings();
		registerTickHandler();
		LOGGER.info("Dawn GUI Reader initialized. TTS available: {}", ttsEngine.isAvailable());
	}

	private static void registerKeyBindings() {
		toggleReaderKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.dawn_accessibility.toggle_reader",
				InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));
		repeatItemKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.dawn_accessibility.repeat_item",
				InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));
		crosshairReadKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.dawn_accessibility.crosshair_read",
				InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));
	}

	private static void registerTickHandler() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			handleKeyBindings(client);

			if (client.player != null) {
				hotbarItemReader.update(client.player.getMainHandItem(), client.player.getInventory().getSelectedSlot());
				blockTargetReader.update();
			} else {
				hotbarItemReader.reset();
				blockTargetReader.reset();
			}

			var screen = config.isEnabled()
					&& (config.isContainerReaderEnabled() || config.isGuiTextReaderEnabled())
					? MinecraftScreenCompat.currentScreen(client)
					: null;
			if (screen instanceof EnchantmentScreen enchantScreen) {
				if (config.isContainerReaderEnabled()) {
					hoveredItemReader.reset();
					var w = client.getWindow();
					int mouseX = (int) (client.mouseHandler.xpos() * screen.width / w.getWidth());
					int mouseY = (int) (client.mouseHandler.ypos() * screen.height / w.getHeight());
					enchantmentScreenReader.update(enchantScreen, mouseX, mouseY);
				} else {
					hoveredItemReader.reset();
					enchantmentScreenReader.reset();
				}
			} else if (screen instanceof AbstractContainerScreen containerScreen) {
				enchantmentScreenReader.reset();
				if (config.isContainerReaderEnabled()) {
					Slot computed = findSlotAt(containerScreen, client);
					hoveredItemReader.update(computed);
				} else {
					hoveredItemReader.reset();
				}
			} else {
				hoveredItemReader.reset();
				enchantmentScreenReader.reset();
			}

			guiTextReader.update(client, screen);
		});
	}

	public static void onInputPress(long windowHandle, InputConstants.Key key) {
		if (toggleReaderKey == null || windowHandle == 0) return;
		Window window = Minecraft.getInstance().getWindow();
		if (windowHandle != window.handle() || !window.isFocused()) return;
		if (toggleReaderKey.matches(key)) toggleClicks++;
		if (repeatItemKey.matches(key)) repeatClicks++;
		if (crosshairReadKey.matches(key)) crosshairClicks++;
	}

	private static void handleKeyBindings(Minecraft client) {
		int toggles = toggleClicks, repeats = repeatClicks, crosshairReads = crosshairClicks;
		toggleClicks = repeatClicks = crosshairClicks = 0;
		if (!client.getWindow().isFocused()) return;
		for (int i = 0; i < toggles; i++) {
			config.setEnabled(!config.isEnabled());
			config.save();
			showStatus(client, config.isEnabled()
					? "message.dawn_accessibility.reader_enabled"
					: "message.dawn_accessibility.reader_disabled");
		}
		for (int i = 0; i < repeats; i++) repeatHoveredItem();
		for (int i = 0; i < crosshairReads; i++) blockTargetReader.readNow();
	}
	private static void showStatus(Minecraft client, String key) {
		if (client.player != null) client.player.sendOverlayMessage(Component.translatable(key));
	}

	public static DawnAccessibilityConfig config() { return config; }
	public static TtsEngine ttsEngine() { return ttsEngine; }
	public static HoveredItemReader hoveredItemReader() { return hoveredItemReader; }
	public static HoveredTextReader hoveredCreativeTabReader() { return hoveredCreativeTabReader; }
	public static HotbarItemReader hotbarItemReader() { return hotbarItemReader; }
	public static BlockTargetReader blockTargetReader() { return blockTargetReader; }
	public static GuiTextReader guiTextReader() { return guiTextReader; }
	public static EnchantmentScreenReader enchantmentScreenReader() { return enchantmentScreenReader; }

	public static KeyMapping toggleReaderKey() { return toggleReaderKey; }
	public static KeyMapping repeatItemKey() { return repeatItemKey; }
	public static KeyMapping crosshairReadKey() { return crosshairReadKey; }

	public static void speak(String text) {
		if (config.isEnabled() && !text.isBlank()) {
			ttsEngine.speak(text, new TtsOptions(config.getSpeechRate(), config.getVolume(), config.getVoiceId()));
		}
	}

	public static void repeatHoveredItem() {
		if (hoveredItemReader.currentItemName().isPresent()) {
			hoveredItemReader.currentItemName().ifPresent(DawnAccessibilityClient::speak);
			return;
		}
		if (hoveredCreativeTabReader.currentText().isPresent()) {
			hoveredCreativeTabReader.currentText().ifPresent(DawnAccessibilityClient::speak);
			return;
		}
		if (hotbarItemReader.currentItemName().isPresent()) {
			hotbarItemReader.currentItemName().ifPresent(DawnAccessibilityClient::speak);
			return;
		}
		if (blockTargetReader.currentBlockName().isPresent()) {
			blockTargetReader.currentBlockName().ifPresent(DawnAccessibilityClient::speak);
			return;
		}
	}

	private static Slot findSlotAt(AbstractContainerScreen screen, Minecraft client) {
		if (screen.getMenu() == null) return null;
		int leftPos = ((AbstractContainerScreenAccessor) screen).dawnAccessibility$getLeftPos();
		int topPos = ((AbstractContainerScreenAccessor) screen).dawnAccessibility$getTopPos();
		var window = client.getWindow();
		int mouseX = (int) (client.mouseHandler.xpos() * screen.width / window.getWidth());
		int mouseY = (int) (client.mouseHandler.ypos() * screen.height / window.getHeight());
		for (var slot : screen.getMenu().slots) {
			int sx = leftPos + slot.x, sy = topPos + slot.y;
			if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) return slot;
		}
		return null;
	}
}
