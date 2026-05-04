package de.miraculixx.veinminerClient.render

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object HUDRenderer: HudElement {
    private val AXE_ICON = Identifier.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/axe.png")
    private val PICKAXE_ICON = Identifier.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/pickaxe.png")
    private val SHOVEL_ICON = Identifier.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/shovel.png")
    private val HOE_ICON = Identifier.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/hoe.png")
    private val FORBIDDEN_ICON = Identifier.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/forbidden.png")
    private var target: Identifier? = null

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
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
}