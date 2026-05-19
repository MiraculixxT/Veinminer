package de.miraculixx.veinminer.pattern

import de.miraculixx.veinminer.data.BlockPosition
import kotlin.math.abs

/**
 * Unified veinmining logic
 */
object Veinmining {

    data class Hit(val pos: BlockPosition, val distance: Int)

    /**
     * Main vinemining entrypoint.
     * Use [shouldBreak]=false for a pure preview
     */
    fun veinmine(
        action: VeinmineAction<*, *>,
        blockAwareness: BlockAwareness,
        shape: Shape,
        maxDepth: Int = Int.MAX_VALUE,
        shouldBreak: Boolean = false,
    ): List<Hit> {
        val origin = action.currentBlock
        val searchRadius = action.settings.searchRadius
        val maxChain = action.settings.maxChain
        val delay = action.settings.delay

        val visited = LinkedHashSet<BlockPosition>()
        val out = ArrayList<Hit>()
        var depthConsumed = 0
        var prevLayer: List<BlockPosition> = listOf(origin)
        var dist = 0

        for (layer in shape.strategy.layers(action, blockAwareness)) {
            if (visited.size >= maxChain) break
            if (depthConsumed >= maxDepth) break
            depthConsumed++
            val matched = ArrayList<BlockPosition>(layer.size)
            for (cand in layer) {
                if (visited.size >= maxChain) break
                if (cand in visited) continue
                if (cand == origin) {
                    visited.add(cand)
                    matched.add(cand)
                    out.add(Hit(cand, 0))
                    continue
                }
                if (!touchesAny(cand, prevLayer, searchRadius)) continue
                visited.add(cand)
                matched.add(cand)
                val distance = ++dist
                out.add(Hit(cand, distance))
                if (shouldBreak) {
                    if (!blockAwareness.breakBlock(cand, delay * distance)) {
                        matched.clear() // block break fail = abort
                        break
                    }
                }
            }
            if (matched.isEmpty()) break
            prevLayer = matched
        }
        return out
    }

    private fun touchesAny(pos: BlockPosition, others: List<BlockPosition>, radius: Int): Boolean {
        for (o in others) {
            val dx = abs(pos.x - o.x)
            val dy = abs(pos.y - o.y)
            val dz = abs(pos.z - o.z)
            if (dx <= radius && dy <= radius && dz <= radius) return true
        }
        return false
    }
}
