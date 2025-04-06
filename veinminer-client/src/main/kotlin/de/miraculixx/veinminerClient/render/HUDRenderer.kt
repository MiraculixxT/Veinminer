package de.miraculixx.veinminerClient.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

object HUDRenderer {
    private val AXE_ICON = ResourceLocation("veinminer-client", "textures/gui/sprite/axe.png")
    private val PICKAXE_ICON = ResourceLocation("veinminer-client", "textures/gui/sprite/pickaxe.png")
    private val SHOVEL_ICON = ResourceLocation("veinminer-client", "textures/gui/sprite/shovel.png")
    private val HOE_ICON = ResourceLocation("veinminer-client", "textures/gui/sprite/hoe.png")
    private val FORBIDDEN_ICON = ResourceLocation("veinminer-client", "textures/gui/sprite/forbidden.png")
    private var target: ResourceLocation? = null

    fun render(guiGraphics: GuiGraphics, deltaTracker: Float) {
        if (target == null) return

        val client = Minecraft.getInstance()
        val window = client.window

        guiGraphics.blit(target, (window.guiScaledWidth / 2) + 2, (window.guiScaledHeight / 2) - 10, 0f, 0f, 8, 8, 8, 8)
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