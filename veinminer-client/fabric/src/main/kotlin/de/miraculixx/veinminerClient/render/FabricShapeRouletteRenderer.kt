package de.miraculixx.veinminerClient.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

object FabricShapeRouletteRenderer {
    fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        ShapeRouletteOverlay.render(graphics, deltaTracker)
    }
}
