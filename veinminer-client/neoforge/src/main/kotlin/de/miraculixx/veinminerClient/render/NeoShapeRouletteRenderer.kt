package de.miraculixx.veinminerClient.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.neoforged.neoforge.client.gui.GuiLayer

object NeoShapeRouletteRenderer : GuiLayer {
    override fun render(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        ShapeRouletteOverlay.render(graphics, deltaTracker)
    }
}
