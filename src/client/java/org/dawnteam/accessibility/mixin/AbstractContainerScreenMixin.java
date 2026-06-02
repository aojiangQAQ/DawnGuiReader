package org.dawnteam.accessibility.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.dawnteam.accessibility.DawnAccessibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow protected int imageWidth;
	@Shadow protected int imageHeight;
	@Shadow protected T menu;

	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void dawnAccessibility$afterRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Slot computed = dawnAccessibility$findSlotAt(mouseX, mouseY);
		DawnAccessibilityClient.hoveredItemReader().update(computed);
		DawnAccessibilityClient.hoveredItemReader().currentItemName().ifPresent(itemName ->
				DawnAccessibilityClient.largeItemNameOverlay().render(
						graphics, itemName, width, height,
						leftPos, topPos, imageWidth, imageHeight
				)
		);
	}

	@Inject(method = "removed", at = @At("HEAD"), require = 0)
	private void dawnAccessibility$removed(CallbackInfo ci) {
		DawnAccessibilityClient.hoveredItemReader().reset();
	}

	@Unique
	private Slot dawnAccessibility$findSlotAt(int mouseX, int mouseY) {
		if (menu == null) return null;
		for (Slot slot : menu.slots) {
			int sx = leftPos + slot.x;
			int sy = topPos + slot.y;
			if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
				return slot;
			}
		}
		return null;
	}
}
