package org.dawnteam.accessibility.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
	protected AbstractContainerScreenMixin(Component title) { super(title); }

	@Inject(method = "removed", at = @At("HEAD"), require = 0)
	private void dawnAccessibility$removed(CallbackInfo ci) {
		DawnAccessibilityClient.hoveredItemReader().reset();
	}
}