package org.dawnteam.accessibility.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MinecraftScreenCompat {
	private static final Logger LOGGER = DawnAccessibilityClient.LOGGER;

	private static boolean lookedUpGuiScreenMethod;
	private static Method guiScreenMethod;
	private static boolean loggedGuiScreenFailure;
	private static boolean lookedUpMinecraftScreenField;
	private static Field minecraftScreenField;
	private static boolean loggedMinecraftScreenFailure;

	private MinecraftScreenCompat() {
	}

	public static Screen currentScreen(Minecraft client) {
		Method method = guiScreenMethod(client);
		if (method != null) {
			try {
				Object value = method.invoke(client.gui);
				return value instanceof Screen screen ? screen : null;
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				logGuiScreenFailure(e);
			}
		}
		return readFromMinecraft(client);
	}

	private static Method guiScreenMethod(Minecraft client) {
		if (client.gui == null) {
			return null;
		}
		if (!lookedUpGuiScreenMethod) {
			lookedUpGuiScreenMethod = true;
			try {
				guiScreenMethod = client.gui.getClass().getMethod("screen");
				guiScreenMethod.setAccessible(true);
			} catch (NoSuchMethodException ignored) {
				guiScreenMethod = null;
			} catch (RuntimeException e) {
				guiScreenMethod = null;
				logGuiScreenFailure(e);
			}
		}
		return guiScreenMethod;
	}

	private static Screen readFromMinecraft(Minecraft client) {
		Field field = minecraftScreenField();
		if (field == null) {
			return null;
		}
		try {
			Object value = field.get(client);
			return value instanceof Screen screen ? screen : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			logMinecraftScreenFailure(e);
			return null;
		}
	}

	private static Field minecraftScreenField() {
		if (!lookedUpMinecraftScreenField) {
			lookedUpMinecraftScreenField = true;
			try {
				minecraftScreenField = Minecraft.class.getDeclaredField("screen");
				minecraftScreenField.setAccessible(true);
			} catch (NoSuchFieldException e) {
				minecraftScreenField = null;
			} catch (RuntimeException e) {
				minecraftScreenField = null;
				logMinecraftScreenFailure(e);
			}
		}
		return minecraftScreenField;
	}

	private static void logGuiScreenFailure(Exception e) {
		if (!loggedGuiScreenFailure) {
			loggedGuiScreenFailure = true;
			LOGGER.warn("Failed to read current screen via Gui.screen(); falling back to Minecraft.screen", e);
		}
	}

	private static void logMinecraftScreenFailure(Exception e) {
		if (!loggedMinecraftScreenFailure) {
			loggedMinecraftScreenFailure = true;
			LOGGER.warn("Failed to read current screen via Minecraft.screen", e);
		}
	}
}
