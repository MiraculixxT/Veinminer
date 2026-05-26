package de.miraculixx.veinminerClient.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

object NeoShapeRouletteRenderer {
    fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        ShapeRouletteOverlay.render(graphics, deltaTracker)
    }
}
