package org.dawnteam.accessibility.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.dawnteam.accessibility.DawnAccessibilityClient;

import java.util.ArrayList;
import java.util.List;

public final class LargeItemNameOverlay {
	private static final int MARGIN = 8;
	private static final int MIN_RIGHT_WIDTH = 96;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int SHADOW_COLOR = 0xFF000000;
	private static final int BACKGROUND_COLOR = 0xD0000000;

	public void render(GuiGraphicsExtractor graphics, String text, int screenWidth, int screenHeight, int containerX, int containerY, int containerWidth, int containerHeight) {
		if (!DawnAccessibilityClient.config().isLargeTextEnabled() || text.isBlank()) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		float scale = Math.max(1.0f, DawnAccessibilityClient.config().getLargeTextSize() / 9.0f);
		int rightX = containerX + containerWidth + MARGIN;
		int rightWidth = screenWidth - rightX - MARGIN;
		boolean useRightSide = rightWidth >= MIN_RIGHT_WIDTH;
		int maxWidth = useRightSide ? rightWidth : Math.max(64, screenWidth - MARGIN * 4);
		List<String> lines = wrap(font, text, (int) (maxWidth / scale));
		int unscaledLineHeight = font.lineHeight + 2;
		int boxWidth = 0;
		for (String line : lines) {
			boxWidth = Math.max(boxWidth, font.width(line));
		}

		int scaledWidth = Math.round(boxWidth * scale);
		int scaledHeight = Math.round(lines.size() * unscaledLineHeight * scale);
		int x = useRightSide ? rightX : Math.max(MARGIN, (screenWidth - scaledWidth) / 2);
		int y = useRightSide ? containerY + MARGIN : Math.min(screenHeight - scaledHeight - MARGIN * 2, containerY + containerHeight + MARGIN);
		y = Math.max(MARGIN, y);

		graphics.nextStratum();
		graphics.fill(
				x - MARGIN,
				y - MARGIN,
				Math.min(screenWidth - MARGIN, x + scaledWidth + MARGIN),
				Math.min(screenHeight - MARGIN, y + scaledHeight + MARGIN),
				BACKGROUND_COLOR
		);

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		for (int i = 0; i < lines.size(); i++) {
			int lineY = i * unscaledLineHeight;
			graphics.text(font, lines.get(i), 1, lineY + 1, SHADOW_COLOR, false);
			graphics.text(font, lines.get(i), 0, lineY, TEXT_COLOR, false);
		}
		graphics.pose().popMatrix();
	}

	private static List<String> wrap(Font font, String text, int maxUnscaledWidth) {
		List<String> lines = new ArrayList<>();
		if (font.width(text) <= maxUnscaledWidth) {
			lines.add(text);
			return lines;
		}

		String[] words = text.split("\\s+");
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			String candidate = current.isEmpty() ? word : current + " " + word;
			if (font.width(candidate) <= maxUnscaledWidth) {
				current.setLength(0);
				current.append(candidate);
			} else {
				if (!current.isEmpty()) {
					lines.add(current.toString());
					current.setLength(0);
				}
				addLongWord(font, word, maxUnscaledWidth, lines, current);
			}
		}

		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines.isEmpty() ? List.of(text) : lines;
	}

	private static void addLongWord(Font font, String word, int maxUnscaledWidth, List<String> lines, StringBuilder current) {
		StringBuilder part = new StringBuilder();
		for (int offset = 0; offset < word.length(); ) {
			int codePoint = word.codePointAt(offset);
			String next = part + new String(Character.toChars(codePoint));
			if (!part.isEmpty() && font.width(next) > maxUnscaledWidth) {
				lines.add(part.toString());
				part.setLength(0);
			}
			part.appendCodePoint(codePoint);
			offset += Character.charCount(codePoint);
		}
		current.append(part);
	}
}
