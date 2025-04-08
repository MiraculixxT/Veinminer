package de.miraculixx.veinminerClient.render

import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

object HUDRenderer {
    private val AXE_ICON = ResourceLocation.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/axe.png")
    private val PICKAXE_ICON = ResourceLocation.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/pickaxe.png")
    private val SHOVEL_ICON = ResourceLocation.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/shovel.png")
    private val HOE_ICON = ResourceLocation.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/hoe.png")
    private val FORBIDDEN_ICON = ResourceLocation.fromNamespaceAndPath("veinminer-client", "textures/gui/sprite/forbidden.png")
    private var target: ResourceLocation? = null

    fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        if (target == null) return

        val client = Minecraft.getInstance()
        val window = client.window

        guiGraphics.blit(RenderType::crosshair, target, (window.guiScaledWidth / 2) + 2, (window.guiScaledHeight / 2) - 10, 0f, 0f, 8, 8, 8, 8)
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