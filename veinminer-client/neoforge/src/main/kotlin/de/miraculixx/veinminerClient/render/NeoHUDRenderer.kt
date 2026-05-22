package de.miraculixx.veinminerClient.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.neoforged.neoforge.client.gui.GuiLayer

object NeoHUDRenderer : HUDRenderer(), GuiLayer {
    override fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        renderCrosshair(graphics, deltaTracker)
    }
}
