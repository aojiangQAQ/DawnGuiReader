package org.dawnteam.accessibility.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.dawnteam.accessibility.gui.DawnClothConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void dawnAccessibility$addOptionsButton(CallbackInfo ci) {
		addRenderableWidget(Button.builder(
						Component.translatable("screen.dawn_accessibility.title"),
						button -> Minecraft.getInstance().setScreen(DawnClothConfigScreen.create((Screen) (Object) this))
				)
				.bounds(width / 2 - 102, height / 4 + 144, 204, 20)
				.build());
	}
}
