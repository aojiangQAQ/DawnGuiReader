package org.dawnteam.accessibility.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void dawnAccessibility$recordPress(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
		if (action == InputConstants.PRESS) {
			DawnAccessibilityClient.onInputPress(window,
					InputConstants.Type.MOUSE.getOrCreate(button.button()));
		}
	}
}
