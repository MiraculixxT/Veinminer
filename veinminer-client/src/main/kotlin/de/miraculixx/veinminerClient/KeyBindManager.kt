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

    fun tick() {
        if (KEY_VEINMINE.isDown) {
            checkBlockTarget()
            scrollPattern()

        } else {
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
        BlockHighlightingRenderer.highlightedBlocks.add(pos.toVeinminer().copy(y = pos.y + 1))
        BlockHighlightingRenderer.highlightedBlocks.add(pos.toVeinminer().copy(y = pos.y + 2))
    }

    // Scroll through veinmine patterns
    fun scrollPattern() {
        // TODO
    }
}