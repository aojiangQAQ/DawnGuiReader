package org.dawnteam.accessibility;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.config.DawnAccessibilityConfig;
import org.dawnteam.accessibility.compat.MinecraftScreenCompat;
import org.dawnteam.accessibility.gui.BlockTargetReader;
import org.dawnteam.accessibility.gui.EnchantmentScreenReader;
import org.dawnteam.accessibility.gui.GuiTextReader;
import org.dawnteam.accessibility.gui.HotbarItemReader;
import org.dawnteam.accessibility.gui.HoveredItemReader;
import org.dawnteam.accessibility.gui.HoveredTextReader;
import org.dawnteam.accessibility.mixin.KeyMappingAccessor;
import org.dawnteam.accessibility.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.dawnteam.accessibility.tts.SystemTtsEngine;
import org.dawnteam.accessibility.tts.TtsEngine;
import org.dawnteam.accessibility.tts.TtsOptions;
import org.lwjgl.glfw.GLFW;
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
	private static final String KEY_CATEGORY = "key.categories.dawn_accessibility.accessibility";

	private static boolean toggleWasDown, repeatWasDown, crosshairWasDown;

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
		toggleReaderKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.dawn_accessibility.toggle_reader",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
		repeatItemKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.dawn_accessibility.repeat_item",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
		crosshairReadKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.dawn_accessibility.crosshair_read",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
	}

	private static void registerTickHandler() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Window window = client.getWindow();

			if (handleKey(toggleReaderKey, window, toggleWasDown)) {
				config.setEnabled(!config.isEnabled());
				config.save();
				showStatus(client, config.isEnabled()
						? "message.dawn_accessibility.reader_enabled"
						: "message.dawn_accessibility.reader_disabled");
			}
			toggleWasDown = isKeyDown(toggleReaderKey, window);

			if (handleKey(repeatItemKey, window, repeatWasDown)) repeatHoveredItem();
			repeatWasDown = isKeyDown(repeatItemKey, window);

			if (handleKey(crosshairReadKey, window, crosshairWasDown)) blockTargetReader.readNow();
			crosshairWasDown = isKeyDown(crosshairReadKey, window);

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

	private static boolean isKeyDown(KeyMapping mapping, Window window) {
		int key = ((KeyMappingAccessor) mapping).dawnAccessibility$getKey().getValue();
		return key != GLFW.GLFW_KEY_UNKNOWN && InputConstants.isKeyDown(window.getWindow(), key);
	}
	private static boolean handleKey(KeyMapping mapping, Window window, boolean wasDown) {
		return isKeyDown(mapping, window) && !wasDown;
	}
	private static void showStatus(Minecraft client, String key) {
		if (client.player != null) client.player.displayClientMessage(Component.translatable(key), true);
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
