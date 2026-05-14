package de.miraculixx.veinminerClient

import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.constants.KeyBindings
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import de.miraculixx.veinminerClient.render.ShapeRouletteOverlay
import java.util.concurrent.atomic.AtomicInteger
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
        val toggleKey = KeyBindings.toggle ?: return
        val holdKey = KeyBindings.hold ?: return

        if (toggleKey.consumeClick()) isToggled = !isToggled
        val currentlyActive = isToggled || holdKey.isDown

        if (currentlyActive) {
            if (!NetworkManager.isVeinminerActive) {
                if (!notifiedOnce) {
                    notifiedOnce = true
                    val mc = Minecraft.getInstance()
                    mc.toastManager.addToast(
                        SystemToast.multiline(
                            mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
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
            HUDProvider.instance.updateTarget(null)
            BlockHighlightingRenderer.setShape(emptyList())
            lastTarget = null
        }
    }

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

        if (target.type != HitResult.Type.BLOCK) {
            resetTarget()
            HUDProvider.instance.updateTarget("forbidden")
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (itemChanged) delay(1.ticks) // wait for server to update the held item
            resetTarget()
            NetworkManager.sendBlockRequest(pos, target.direction)
        }
    }

    private fun resetTarget() {
        BlockHighlightingRenderer.setShape(emptyList())
    }

    private val pendingScroll = AtomicInteger(0)

    fun queueScroll(delta: Int) {
        if (delta == 0) return
        pendingScroll.addAndGet(if (delta > 0) 1 else -1)
    }

    fun scrollPattern() {
        val raw = pendingScroll.getAndSet(0)
        if (raw == 0) return
        val delta = -raw // wheel up advances forward through Shape.entries
        ShapeRouletteOverlay.onScroll(delta)
        NetworkManager.selectedShape = ShapeRouletteOverlay.currentShape
        NetworkManager.resendKeyPress()
        lastTarget = null
    }

    fun onDisconnect() {
        notifiedOnce = false
    }
}
