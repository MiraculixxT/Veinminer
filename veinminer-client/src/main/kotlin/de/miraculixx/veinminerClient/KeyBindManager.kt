package de.miraculixx.veinminerClient

import de.miraculixx.veinminerClient.constants.KEY_VEINMINE
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult

object KeyBindManager {
    private var lastTarget: BlockPos? = null

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
        lastTarget = pos

        // Request vein for block highlighting and hud
        // TODO
    }

    // Scroll through veinmine patterns
    fun scrollPattern() {
        // TODO
    }
}