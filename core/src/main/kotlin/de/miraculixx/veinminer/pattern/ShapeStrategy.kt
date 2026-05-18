package de.miraculixx.veinminer.pattern

import de.miraculixx.veinminer.data.BlockPosition

/**
 * Generates candidate positions for a [Shape], grouped into ordered **layers**
 * stepping away from the source block. The consumer walks layers in order,
 * checks block-state matches and per-layer adjacency against the previous
 * matched set, and stops as soon as a layer yields no matches (the tunnel
 * is "cut off" by non-target blocks).
 *
 * Sequence is lazy: callers can abort at any layer without computing the tail.
 */
fun interface ShapeStrategy {
    fun layers(origin: BlockPosition, face: Surface, maxChain: Int): Sequence<List<BlockPosition>>
}

/**
 * Use optimized per implementation flood fill with world awareness
 */
object NormalStrategy : ShapeStrategy {
    override fun layers(origin: BlockPosition, face: Surface, maxChain: Int): Sequence<List<BlockPosition>> =
        emptySequence()
}

/**
 * Single-layer flood-fill on the plane perpendicular to the drill axis
 */
object FlatStrategy : ShapeStrategy {
    override fun layers(origin: BlockPosition, face: Surface, maxChain: Int): Sequence<List<BlockPosition>> = sequence {
        val (u, v) = face.basisVectors()
        yield(listOf(origin))
        var ring = 1
        while (ring <= maxChain) {
            val layer = ArrayList<BlockPosition>(ring * 8)
            for (du in -ring..ring) {
                for (dv in -ring..ring) {
                    if (maxOf(kotlin.math.abs(du), kotlin.math.abs(dv)) != ring) continue
                    layer.add(
                        BlockPosition(
                            origin.x + u.first * du + v.first * dv,
                            origin.y + u.second * du + v.second * dv,
                            origin.z + u.third * du + v.third * dv,
                        )
                    )
                }
            }
            yield(layer)
            ring++
        }
    }
}

/**
 * With size=N, a NxN tunnel in the direction of the drill surface
 */
class TunnelStrategy(private val size: Int) : ShapeStrategy {
    override fun layers(origin: BlockPosition, face: Surface, maxChain: Int): Sequence<List<BlockPosition>> = sequence {
        val drill = face.normalInward()
        val (u, v) = face.basisVectors()
        val area = size * size
        val maxDepth = (maxChain / area).coerceAtLeast(1)
        val lo = -(size - 1) / 2
        val hi = lo + size - 1
        for (depth in 0 until maxDepth) {
            val layer = ArrayList<BlockPosition>(area)
            for (du in lo..hi) {
                for (dv in lo..hi) {
                    layer.add(
                        BlockPosition(
                            origin.x + drill.first * depth + u.first * du + v.first * dv,
                            origin.y + drill.second * depth + u.second * du + v.second * dv,
                            origin.z + drill.third * depth + u.third * du + v.third * dv,
                        )
                    )
                }
            }
            yield(layer)
        }
    }
}

fun Surface.normalOutward(): Triple<Int, Int, Int> = when (this) {
    Surface.UP -> Triple(0, 1, 0)
    Surface.DOWN -> Triple(0, -1, 0)
    Surface.NORTH -> Triple(0, 0, -1)
    Surface.SOUTH -> Triple(0, 0, 1)
    Surface.WEST -> Triple(-1, 0, 0)
    Surface.EAST -> Triple(1, 0, 0)
}

fun Surface.normalInward(): Triple<Int, Int, Int> {
    val (x, y, z) = normalOutward()
    return Triple(-x, -y, -z)
}

fun Surface.basisVectors(): Pair<Triple<Int, Int, Int>, Triple<Int, Int, Int>> = when (this) {
    Surface.UP, Surface.DOWN -> Triple(1, 0, 0) to Triple(0, 0, 1)
    Surface.NORTH, Surface.SOUTH -> Triple(1, 0, 0) to Triple(0, 1, 0)
    Surface.EAST, Surface.WEST -> Triple(0, 0, 1) to Triple(0, 1, 0)
}
