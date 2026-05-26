package de.miraculixx.veinminerClient.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

object NeoHUDRenderer : HUDRenderer() {
    fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        renderCrosshair(graphics, deltaTracker)
    }
}
