package org.dawnteam.accessibility.mixin;

import com.mojang.text2speech.Narrator;
import net.minecraft.client.GameNarrator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameNarrator.class)
public interface GameNarratorAccessor {
	@Accessor("narrator")
	Narrator dawnAccessibility$getNarrator();
}
