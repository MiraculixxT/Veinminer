package de.miraculixx.veinminer.pattern

import de.miraculixx.veinminer.data.BlockPosition
import kotlin.math.abs

/**
 * Pure block-selection algorithms shared between server (authoritative break) and client (highlight).
 * Loader passes an `isMatch` predicate that resolves a position against its native world view.
 *
 * Result entries are ordered (BFS / per-layer); `distance` is the step index from origin and may be
 * used by callers to stagger break-delay timing.
 */
object VeinSelector {

    data class Hit(val pos: BlockPosition, val distance: Int)

    fun floodFill(
        origin: BlockPosition,
        isMatch: (BlockPosition) -> Boolean,
        maxChain: Int,
        searchRadius: Int,
    ): List<Hit> {
        val visited = LinkedHashMap<BlockPosition, Int>()
        val queue = ArrayDeque<Hit>()
        queue.add(Hit(origin, 0))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.pos in visited) continue
            if (visited.size >= maxChain) break
            visited[current.pos] = current.distance

            val nextDist = current.distance + 1
            for (x in -searchRadius..searchRadius) {
                for (y in -searchRadius..searchRadius) {
                    for (z in -searchRadius..searchRadius) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val next = BlockPosition(current.pos.x + x, current.pos.y + y, current.pos.z + z)
                        if (next in visited) continue
                        if (!isMatch(next)) continue
                        queue.add(Hit(next, nextDist))
                    }
                }
            }
        }
        return visited.map { Hit(it.key, it.value) }
    }

    fun shapeFill(
        origin: BlockPosition,
        face: Surface,
        shape: Shape,
        isMatch: (BlockPosition) -> Boolean,
        maxChain: Int,
        searchRadius: Int,
        maxDepth: Int,
    ): List<Hit> {
        val visited = LinkedHashSet<BlockPosition>()
        val out = ArrayList<Hit>()
        var depthConsumed = 0
        var prevLayer: List<BlockPosition> = listOf(origin)
        var dist = 0

        for (layer in shape.strategy.layers(origin, face, maxChain)) {
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
                if (!isMatch(cand)) continue
                visited.add(cand)
                matched.add(cand)
                out.add(Hit(cand, ++dist))
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
