package de.miraculixx.veinminerClient

import com.mojang.authlib.minecraft.client.MinecraftClient
import de.miraculixx.veinminerClient.constants.KEY_VEINMINE
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.BlockHitResult
import net.silkmc.silk.core.Silk

object KeyBindManager {
    var lastTarget: BlockPos? = null
        private set
    var isPressed = false
        private set(value) {
            NetworkManager.sendKeyPress(value)
            field = value
        }
    private var notifiedOnce = false


    fun tick() {
        if (KEY_VEINMINE.isDown) {
            // Notify user if not active
            if (!NetworkManager.isVeinminerActive) {
                if (!notifiedOnce) {
                    notifiedOnce = true
                    val mc = Minecraft.getInstance()
                    mc.toastManager.addToast(
                        SystemToast.multiline(mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("toast.disabled.title"),
                            Component.translatable("toast.disabled.subtitle")
                        )
                    )
                }
                return
            }

            if (!isPressed) isPressed = true
            checkBlockTarget()
            scrollPattern()

        } else {
            if (isPressed) isPressed = false
            HUDRenderer.updateTarget(null)
            BlockHighlightingRenderer.setShape(emptyList())
            lastTarget = null
        }
    }

    // Check current block target & update if necessary
    fun checkBlockTarget() {
        val instance = Minecraft.getInstance()
        val target = instance.hitResult as? BlockHitResult ?: return
        val pos = target.blockPos
        if (pos == lastTarget) return
        lastTarget = pos

        // Request vein for block highlighting and hud
        BlockHighlightingRenderer.setShape(emptyList())
        NetworkManager.requestBlockInfo(pos, target.direction)
    }

    // Scroll through veinmine patterns
    fun scrollPattern() {
        // TODO
    }

    fun onDisconnect() {
        notifiedOnce = false
    }
}