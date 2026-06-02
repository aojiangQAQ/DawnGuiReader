package org.dawnteam.accessibility.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
	@Unique
	private boolean dawnAccessibility$hoveredCreativeTabThisFrame;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void dawnAccessibility$beginCreativeTabHoverFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		dawnAccessibility$hoveredCreativeTabThisFrame = false;
	}

	@Inject(method = "checkTabHovering", at = @At("RETURN"))
	private void dawnAccessibility$readHoveredCreativeTab(GuiGraphicsExtractor graphics, CreativeModeTab tab, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
		if (Boolean.TRUE.equals(cir.getReturnValue())) {
			dawnAccessibility$hoveredCreativeTabThisFrame = true;
			DawnAccessibilityClient.hoveredCreativeTabReader().update(tab.getDisplayName().getString());
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void dawnAccessibility$endCreativeTabHoverFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!dawnAccessibility$hoveredCreativeTabThisFrame) {
			DawnAccessibilityClient.hoveredCreativeTabReader().reset();
		}
	}

	@Inject(method = "removed", at = @At("HEAD"), require = 0)
	private void dawnAccessibility$removed(CallbackInfo ci) {
		DawnAccessibilityClient.hoveredCreativeTabReader().reset();
	}
}
