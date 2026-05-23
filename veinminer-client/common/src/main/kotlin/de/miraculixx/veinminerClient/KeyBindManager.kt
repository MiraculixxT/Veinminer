package de.miraculixx.veinminerClient

import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.utils.toVeinminer
import de.miraculixx.veinminerClient.config.ClientPatternConfig
import de.miraculixx.veinminerClient.config.PatternConfigScreen
import de.miraculixx.veinminerClient.constants.KeyBindings
import de.miraculixx.veinminerClient.mining.ClientVeinSelector
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import de.miraculixx.veinminerClient.render.ShapeRouletteOverlay
import java.util.concurrent.atomic.AtomicInteger
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
    private var lastItem: ItemStack? = null
    private var lastFace: Surface = Surface.UP
    var isPressed = false
        private set(value) {
            NetworkManager.sendKeyPress(value, lastFace)
            field = value
        }
    private var isToggled = false
    var notifiedOnce = false

    fun tick() {
        val toggleKey = KeyBindings.toggle ?: return
        val holdKey = KeyBindings.hold ?: return
        if (KeyBindings.config?.consumeClick() == true) {
            Minecraft.getInstance().setScreen(PatternConfigScreen(Minecraft.getInstance().screen))
        }

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
            resetTarget(true)
            lastTarget = null
        }
    }

    fun checkBlockTarget() {
        val instance = Minecraft.getInstance()
        val player = instance.player ?: return
        val level = instance.level ?: return
        val target = instance.hitResult as? BlockHitResult ?: return
        val pos = target.blockPos
        val holding = player.inventory.selectedItem
        if (holding.item == Items.AIR) return resetTarget(true)

        if (target.type != HitResult.Type.BLOCK) {
            resetTarget()
            return
        }

        val face = target.direction.toVeinminer()
        val itemChanged = holding != lastItem
        val faceChanged = face != lastFace
        val targetChanged = pos != lastTarget
        if (!itemChanged && !faceChanged && !targetChanged) return

        lastTarget = pos
        lastItem = holding
        if (faceChanged) {
            lastFace = face
            NetworkManager.resendKeyPress(face)
        }

        val result = ClientVeinSelector.resolve(level, player, pos, face, NetworkManager.selectedPattern, NetworkManager.selectedDepth)

        if (result == null) {
            resetTarget()
            return
        }
        HUDProvider.instance.updateTarget(result.toolIcon)
        BlockHighlightingRenderer.setShape(result.positions)
    }

    private fun resetTarget(hide: Boolean = false) {
        BlockHighlightingRenderer.setShape(emptyList())
        if (hide) HUDProvider.instance.updateTarget(null)
        else HUDProvider.instance.updateTarget("forbidden")
    }

    private val pendingShapeScroll = AtomicInteger(0)
    private val pendingDepthScroll = AtomicInteger(0)

    fun queueScroll(delta: Int, shift: Boolean) {
        if (delta == 0) return
        val sign = if (delta > 0) 1 else -1
        if (shift) pendingDepthScroll.addAndGet(sign) else pendingShapeScroll.addAndGet(sign)
    }

    fun scrollPattern() {
        val rawShape = pendingShapeScroll.getAndSet(0)
        val rawDepth = pendingDepthScroll.getAndSet(0)
        if (rawShape == 0 && rawDepth == 0) return
        var dirty = false
        if (rawShape != 0) {
            ShapeRouletteOverlay.onScroll(if (ClientPatternConfig.settings.invertedScroll) rawShape else -rawShape)
            NetworkManager.selectedPattern = ShapeRouletteOverlay.currentPattern
            dirty = true
        }
        if (rawDepth != 0) {
            ShapeRouletteOverlay.onDepthScroll(if (ClientPatternConfig.settings.invertedScroll) rawDepth else -rawDepth)
            if (NetworkManager.selectedDepth != ShapeRouletteOverlay.currentDepth) {
                NetworkManager.selectedDepth = ShapeRouletteOverlay.currentDepth
                dirty = true
            }
        }
        if (dirty) {
            NetworkManager.resendKeyPress(lastFace)
            lastTarget = null
        }
    }

    fun onDisconnect() {
        notifiedOnce = false
        isPressed = false
        lastTarget = null
        isToggled = false
    }
}
