package de.miraculixx.veinminerClient

import de.miraculixx.veinminerClient.constants.KEY_VEINMINE_HOLD
import de.miraculixx.veinminerClient.constants.KEY_VEINMINE_TOGGLE
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.silkmc.silk.core.kotlin.ticks

object KeyBindManager {
    var lastTarget: BlockPos? = null
        private set
    var lastItem: ItemStack? = null
        private set
    var isPressed = false
        private set(value) {
            NetworkManager.sendKeyPress(value)
            field = value
        }
    private var isToggled = false
    var notifiedOnce = false


    fun tick() {
        if (KEY_VEINMINE_TOGGLE.consumeClick()) isToggled = !isToggled
        val currentlyActive = isToggled || KEY_VEINMINE_HOLD.isDown

        if (currentlyActive) {
            // Notify user if not active
            if (!NetworkManager.isVeinminerActive) {
                if (!notifiedOnce) {
                    notifiedOnce = true
                    val mc = Minecraft.getInstance()
                    mc.toastManager.addToast(
                        SystemToast.multiline(mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("veinminer.disabled.title"),
                            Component.translatable("veinminer.disabled.subtitle")
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
        val holding = instance.player?.inventory?.selectedItem ?: return
        if (holding.item == Items.AIR) return resetTarget()

        val itemChanged = holding != lastItem
        if (pos == lastTarget && !itemChanged) return
        lastTarget = pos
        lastItem = holding

        // If not targeting block, fail
        if (target.type != HitResult.Type.BLOCK) {
            resetTarget()
            HUDRenderer.updateTarget("forbidden")
            return
        }

        // Request vein for block highlighting and hud
        CoroutineScope(Dispatchers.Default).launch {
            if (itemChanged) delay(1.ticks) // Delay 1 tick, as server is slower then client
            resetTarget()
            NetworkManager.sendBlockRequest(pos, target.direction)
        }
    }

    private fun resetTarget() {
        BlockHighlightingRenderer.setShape(emptyList())
    }

    // Scroll through veinmine patterns
    fun scrollPattern() {
        // TODO
    }

    fun onDisconnect() {
        notifiedOnce = false
    }
}