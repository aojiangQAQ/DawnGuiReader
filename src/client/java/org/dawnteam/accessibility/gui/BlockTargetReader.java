package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.Optional;

public final class BlockTargetReader {
	private BlockPos lastPos = null;
	private String lastBlockName = "";
	private String currentBlockName = "";
	private long hoverStartedAtMs;
	private boolean spokenForCurrent;

	public void update() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
			if (!currentBlockName.isEmpty()) reset();
			return;
		}

		BlockHitResult blockHit = (BlockHitResult) client.hitResult;
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = client.level.getBlockState(pos);
		if (state.isAir()) {
			if (!currentBlockName.isEmpty()) reset();
			return;
		}

		String blockName = state.getBlock().getName().getString();
		if (!pos.equals(lastPos) || !blockName.equals(lastBlockName)) {
			lastPos = pos;
			lastBlockName = blockName;
			currentBlockName = blockName;
			hoverStartedAtMs = System.currentTimeMillis();
			spokenForCurrent = false;
		}

		if (!spokenForCurrent
				&& DawnAccessibilityClient.config().getCrosshairMode() == 1
				&& DawnAccessibilityClient.config().isEnabled()
				&& System.currentTimeMillis() - hoverStartedAtMs >= DawnAccessibilityClient.config().getBlockDelayMs()) {
			spokenForCurrent = true;
			DawnAccessibilityClient.speak(blockName);
		}
	}

	public void readNow() {
		if (DawnAccessibilityClient.config().getCrosshairMode() == 0) return;
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) return;
		BlockHitResult blockHit = (BlockHitResult) client.hitResult;
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = client.level.getBlockState(pos);
		if (state.isAir()) return;
		DawnAccessibilityClient.speak(state.getBlock().getName().getString());
	}

	public void reset() {
		lastPos = null;
		lastBlockName = "";
		currentBlockName = "";
		hoverStartedAtMs = 0L;
		spokenForCurrent = false;
	}

	public Optional<String> currentBlockName() {
		return currentBlockName.isBlank() ? Optional.empty() : Optional.of(currentBlockName);
	}
}
