package de.miraculixx.veinminer.pattern

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.VeinminerSettings
import kotlin.math.abs

/**
 * Block traversal algorithms that returns an abortable sequence.
 * Uses [BlockAwareness] for context awareness.
 */
fun interface ShapeStrategy {
    fun layers(veinmineAction: VeinmineAction<*, *>, blockAwareness: BlockAwareness): Sequence<List<BlockPosition>>

    fun requiresLayerConnectivity(layerDepth: Int): Boolean = true
}

fun VeinminerSettings.saveSearchRadius(): Int = searchRadius.coerceIn(1, 5)

/**
 * Layered flood fill in 3D
 */
object NormalStrategy : ShapeStrategy {
    override fun layers(veinmineAction: VeinmineAction<*, *>, blockAwareness: BlockAwareness): Sequence<List<BlockPosition>> = sequence {
        val searchRadius = veinmineAction.settings.saveSearchRadius()
        val maxChain = veinmineAction.settings.maxChain
        val targets = veinmineAction.targetTypes

        val visited = LinkedHashSet<BlockPosition>()
        val queue = ArrayDeque<Pair<BlockPosition, Int>>()
        queue.add(veinmineAction.currentBlock to 0)

        var currentDistance = 0
        var currentLayer = ArrayList<BlockPosition>()

        while (queue.isNotEmpty() && visited.size < maxChain) {
            val (pos, distance) = queue.removeFirst()
            if (!visited.add(pos)) continue

            if (distance != currentDistance) {
                if (currentLayer.isNotEmpty()) yield(currentLayer)
                currentLayer = ArrayList()
                currentDistance = distance
            }
            currentLayer.add(pos)

            val nextDist = distance + 1
            for (x in -searchRadius..searchRadius) {
                for (y in -searchRadius..searchRadius) {
                    for (z in -searchRadius..searchRadius) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val next = BlockPosition(pos.x + x, pos.y + y, pos.z + z)
                        if (next in visited) continue
                        if (targets.isNotEmpty()) {
                            val blockType = blockAwareness.getBlockType(next)
                            if (blockType !in targets) continue
                        }
                        queue.add(next to nextDist)
                    }
                }
            }
        }

        if (currentLayer.isNotEmpty()) yield(currentLayer)
    }
}

/**
 * Single-layer flood-fill on the plane perpendicular to the drill axis
 */
object FlatStrategy : ShapeStrategy {
    override fun layers(veinmineAction: VeinmineAction<*, *>, blockAwareness: BlockAwareness): Sequence<List<BlockPosition>> = sequence {
        val face = veinmineAction.face
        val origin = veinmineAction.sourceLocation
        val maxChain = veinmineAction.settings.maxChain
        val targets = veinmineAction.targetTypes

        val (u, v) = face.basisVectors()
        yield(listOf(origin))
        var ring = 1
        while (ring <= maxChain) {
            val layer = ArrayList<BlockPosition>(ring * 8)
            for (du in -ring..ring) {
                for (dv in -ring..ring) {
                    if (maxOf(abs(du), abs(dv)) != ring) continue
                    val next = BlockPosition(
                        origin.x + u.first * du + v.first * dv,
                        origin.y + u.second * du + v.second * dv,
                        origin.z + u.third * du + v.third * dv,
                    )
                    val blockType = blockAwareness.getBlockType(next)
                    if (blockType !in targets) continue
                    layer.add(next)
                }
            }
            yield(layer)
            ring++
        }
    }
}

/**
 * Rectangular traversal in the direction of the drill surface.
 * Width follows the first surface basis vector, height follows the second.
 * Overriding [heightOffsetAt] transforms the tunnel into a direction (e.g. stair stepping)
 */
abstract class TunnelLikeStrategy(
    private val width: Int,
    private val height: Int,
    strategyName: String,
) : ShapeStrategy {
    init {
        require(width > 0) { "$strategyName width must be positive ($width)" }
        require(height > 0) { "$strategyName height must be positive ($height)" }
    }

    override fun requiresLayerConnectivity(layerDepth: Int): Boolean = layerDepth > 0

    override fun layers(veinmineAction: VeinmineAction<*, *>, blockAwareness: BlockAwareness): Sequence<List<BlockPosition>> = sequence {
        val face = veinmineAction.face
        val origin = veinmineAction.sourceLocation
        val maxChain = veinmineAction.settings.maxChain
        val targets = veinmineAction.targetTypes

        val drill = face.normalInward()
        val (u, v) = face.basisVectors()
        val area = width * height
        val maxDepth = (maxChain / area).coerceAtLeast(1)
        val widthRange = centeredOffsets(width)
        val heightRange = centeredOffsets(height)
        for (depth in 0 until maxDepth) {
            val layer = ArrayList<BlockPosition>(area)
            val heightOffset = heightOffsetAt(depth)
            for (du in widthRange) {
                for (dv in heightRange) {
                    val steppedDv = dv + heightOffset
                    val next = BlockPosition(
                        origin.x + drill.first * depth + u.first * du + v.first * steppedDv,
                        origin.y + drill.second * depth + u.second * du + v.second * steppedDv,
                        origin.z + drill.third * depth + u.third * du + v.third * steppedDv,
                    )
                    val blockType = blockAwareness.getBlockType(next)
                    if (blockType !in targets) continue
                    layer.add(next)
                }
            }
            yield(layer)
        }
    }

    protected open fun heightOffsetAt(depth: Int): Int = 0
}

/**
 * Rectangular tunnel in the direction of the drill surface.
 */
class TunnelStrategy(width: Int, height: Int) : TunnelLikeStrategy(width, height, "Tunnel") {
    constructor(size: Int) : this(size, size)
}

/**
 * Rectangular tunnel that steps by one block along the height axis each depth.
 */
class StairsStrategy(private val relativeUp: Boolean, width: Int, height: Int) : TunnelLikeStrategy(width, height, "Stairs") {
    constructor(relativeUp: Boolean, size: Int) : this(relativeUp, size, size)

    override fun heightOffsetAt(depth: Int): Int = depth * if (relativeUp) 1 else -1
}

private fun centeredOffsets(size: Int): IntRange {
    val lo = -(size - 1) / 2
    return lo..<lo + size
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
