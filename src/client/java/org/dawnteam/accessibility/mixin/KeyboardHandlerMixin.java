package org.dawnteam.accessibility.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void dawnAccessibility$recordPress(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (action == InputConstants.PRESS) {
			DawnAccessibilityClient.onInputPress(window, InputConstants.getKey(event));
		}
	}
}
