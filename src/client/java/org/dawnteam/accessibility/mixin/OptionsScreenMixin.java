package org.dawnteam.accessibility.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.gui.DawnClothConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	protected OptionsScreenMixin(Component title) { super(title); }

	@Inject(method = "init", at = @At("TAIL"))
	private void dawnAccessibility$addButton(CallbackInfo ci) {
		addRenderableWidget(Button.builder(
				Component.translatable("screen.dawn_accessibility.title"),
				b -> Minecraft.getInstance().setScreen(DawnClothConfigScreen.create((Screen)(Object)this))
		).bounds(width / 2 - 100, height / 4 + 120 + 12, 200, 20).build());
	}
}