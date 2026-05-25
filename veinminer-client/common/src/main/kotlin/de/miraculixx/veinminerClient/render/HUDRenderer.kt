package de.miraculixx.veinminerClient.render

import de.miraculixx.veinminerClient.ClientLifecycle
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

abstract class HUDRenderer {
    private val AXE_ICON = icon("axe")
    private val PICKAXE_ICON = icon("pickaxe")
    private val SHOVEL_ICON = icon("shovel")
    private val HOE_ICON = icon("hoe")
    private val FORBIDDEN_ICON = icon("forbidden")
    private var target: Identifier? = null

    fun renderCrosshair(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        if (target == null) return

        val client = Minecraft.getInstance()
        val window = client.window

        graphics.blit(RenderPipelines.CROSSHAIR, target!!, (window.guiScaledWidth / 2) + 2, (window.guiScaledHeight / 2) - 10, 0f, 0f, 8, 8, 8, 8)
    }

    fun updateTarget(target: String?) {
        this.target = when (target) {
            null -> null
            "axe" -> AXE_ICON
            "shovel" -> SHOVEL_ICON
            "hoe" -> HOE_ICON
            "forbidden" -> FORBIDDEN_ICON
            else -> PICKAXE_ICON
        }
    }

    private fun icon(tool: String) = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "textures/gui/sprites/tooltip/${tool}.png")
}
