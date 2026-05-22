package de.miraculixx.veinminerClient.render

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

object FabricHUDRenderer : HUDRenderer(), HudElement {
    override fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        renderCrosshair(graphics, deltaTracker)
    }
}
