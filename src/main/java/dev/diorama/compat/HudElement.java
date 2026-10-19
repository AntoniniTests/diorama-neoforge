package dev.diorama.compat;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

// A HUD element: the same shape as NeoForge's LayeredDraw.Layer, which DioramaClient registers
// these as.
public interface HudElement {
	void render(GuiGraphics graphics, DeltaTracker deltaTracker);
}
