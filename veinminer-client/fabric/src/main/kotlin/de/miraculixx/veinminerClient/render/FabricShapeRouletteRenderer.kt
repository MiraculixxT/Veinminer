package de.miraculixx.veinminerClient.render

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

object FabricShapeRouletteRenderer : HudElement {
    override fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        ShapeRouletteOverlay.render(graphics, deltaTracker)
    }
}
