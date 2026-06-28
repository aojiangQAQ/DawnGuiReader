package org.dawnteam.accessibility.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.gui.DawnClothConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Component title) { super(title); }

	@Inject(method = "init", at = @At("TAIL"))
	private void dawnAccessibility$addButton(CallbackInfo ci) {
		// Place button at bottom-left to avoid overlapping vanilla buttons
		addRenderableWidget(Button.builder(
				Component.translatable("screen.dawn_accessibility.title"),
				b -> Minecraft.getInstance().setScreenAndShow(DawnClothConfigScreen.create((Screen)(Object)this))
		).bounds(4, height - 24, 120, 20).build());
	}
}