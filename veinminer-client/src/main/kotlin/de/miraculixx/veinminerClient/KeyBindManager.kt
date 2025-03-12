package de.miraculixx.veinminerClient

import de.miraculixx.veinminer.config.extensions.toVeinminer
import de.miraculixx.veinminer.config.network.BlockHighlighting
import de.miraculixx.veinminerClient.constants.KEY_VEINMINE
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult

object KeyBindManager {
    var lastTarget: BlockPos? = null
        private set
    var isPressed = false
        private set(value) {
            NetworkManager.sendKeyPress(value)
            field = value
        }


    fun tick() {
        if (KEY_VEINMINE.isDown) {
            if (!isPressed) isPressed = true
            checkBlockTarget()
            scrollPattern()

        } else {
            if (isPressed) isPressed = false
            lastTarget = null
        }
    }

    // Check current block target & update if necessary
    fun checkBlockTarget() {
        val instance = Minecraft.getInstance()
        val target = instance.hitResult as? BlockHitResult ?: return
        val pos = target.blockPos
        if (pos == lastTarget) return
        println("Target: $pos")
        lastTarget = pos

        // Request vein for block highlighting and hud
        NetworkManager.requestBlockInfo(pos, target.direction)

        // DEBUG
        BlockHighlightingRenderer.highlightedBlocks.clear()
        BlockHighlightingRenderer.highlightedBlocks.add(pos.toVeinminer())
    }

    // Scroll through veinmine patterns
    fun scrollPattern() {
        // TODO
    }
}