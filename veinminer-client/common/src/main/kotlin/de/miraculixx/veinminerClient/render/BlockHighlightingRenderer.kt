package de.miraculixx.veinminerClient.render

import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.CompareOp
import com.mojang.blaze3d.vertex.PoseStack
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.oit.OitPipelineSet
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.concurrent.Volatile

object BlockHighlightingRenderer {

    @Volatile
    private var activeEdgeShapes: List<VoxelShape> = emptyList()

    @Volatile
    private var activeFillShapes: List<CulledFillEntry> = emptyList()

    private class CulledFillEntry(val offset: Vec3, val shape: VoxelShape)

    private val renderHighlightingTranslucent: RenderType by lazy {
        RenderType.create(
            "${ClientLifecycle.MOD_ID}:highlight_translucent",
            RenderSetup.builder(
                RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                        .withLocation(
                            Identifier.fromNamespaceAndPath(
                                ClientLifecycle.MOD_ID,
                                "pipeline/highlight_translucent"
                            )
                        )
                        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false)
                        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT)).build()
                )
            ).setOitPipelines(
                RenderPipelines.register(
                    OitPipelineSet.builder(
                        "${ClientLifecycle.MOD_ID}_highlight_translucent",
                        RenderPipeline.builder(RenderPipelines.OIT_LINES_SNIPPET)
                    ).withoutDepthTest().build()
                )
            ).createRenderSetup()
        )
    }

    fun render(stack: PoseStack, collector: OrderedSubmitNodeCollector, camPos: Vec3) {
        val targetBlock = KeyBindManager.lastTarget
        val edgeShapes = activeEdgeShapes
        val fills = activeFillShapes

        if ((edgeShapes.isEmpty() && fills.isEmpty()) || targetBlock == null) return

        val baseCamX = targetBlock.x - camPos.x
        val baseCamY = targetBlock.y - camPos.y
        val baseCamZ = targetBlock.z - camPos.z

        stack.pushPose()
        try {
            stack.translate(baseCamX, baseCamY, baseCamZ)
            for (edgeShape in edgeShapes) {
                collector.submitShapeOutline(stack, edgeShape, RenderTypes.lines(), ARGB.white(255), 1.0f, false)
            }
            if (NetworkManager.settings.client.translucentBlockHighlight) {
                for (fill in fills) {
                    stack.pushPose()
                    try {
                        stack.translate(fill.offset.x, fill.offset.y, fill.offset.z)
                        collector.submitShapeOutline(
                            stack, fill.shape, renderHighlightingTranslucent, ARGB.white(50), 1.0f, true
                        )
                    } finally {
                        stack.popPose()
                    }
                }
            }
        } finally {
            stack.popPose()
        }
    }

    fun setShape(positions: List<BlockPosition>) {
        val source = KeyBindManager.lastTarget
        if (positions.isEmpty() || source == null) {
            activeEdgeShapes = emptyList()
            activeFillShapes = emptyList()
            return
        }

        fun packPos(x: Int, y: Int, z: Int): Long {
            return (x.toLong() and 0x3FFFFFFL shl 38) or (z.toLong() and 0x3FFFFFFL shl 12) or (y.toLong() and 0xFFFL)
        }

        val packedSet = HashSet<Long>(positions.size)
        for (pos in positions) {
            packedSet.add(packPos(pos.x, pos.y, pos.z))
        }

        val newEdgeShapes = mutableListOf<VoxelShape>()
        val newFills = mutableListOf<CulledFillEntry>()

        fun createLineShape(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): VoxelShape {
            return Shapes.box(
                minOf(x1, x2) - THICK,
                minOf(y1, y2) - THICK,
                minOf(z1, z2) - THICK,
                maxOf(x1, x2) + THICK,
                maxOf(y1, y2) + THICK,
                maxOf(z1, z2) + THICK
            )
        }

        for (pos in positions) {
            val px = pos.x
            val py = pos.y
            val pz = pos.z

            val rx = (px - source.x).toDouble()
            val ry = (py - source.y).toDouble()
            val rz = (pz - source.z).toDouble()

            val hasUp = packedSet.contains(packPos(px, py + 1, pz))
            val hasDown = packedSet.contains(packPos(px, py - 1, pz))
            val hasNorth = packedSet.contains(packPos(px, py, pz - 1))
            val hasSouth = packedSet.contains(packPos(px, py, pz + 1))
            val hasWest = packedSet.contains(packPos(px - 1, py, pz))
            val hasEast = packedSet.contains(packPos(px + 1, py, pz))

            val xMin = rx - PAD
            val xMax = rx + 1.0 + PAD
            val yMin = ry - PAD
            val yMax = ry + 1.0 + PAD
            val zMin = rz - PAD
            val zMax = rz + 1.0 + PAD
            if (!hasUp) {
                if (!hasNorth && !packedSet.contains(packPos(px, py + 1, pz - 1)))
                    newEdgeShapes.add(createLineShape(xMin, yMax, zMin, xMax, yMax, zMin))
                if (!hasSouth && !packedSet.contains(packPos(px, py + 1, pz + 1)))
                    newEdgeShapes.add(createLineShape(xMin, yMax, zMax, xMax, yMax, zMax))
                if (!hasWest && !packedSet.contains(packPos(px - 1, py + 1, pz)))
                    newEdgeShapes.add(createLineShape(xMin, yMax, zMin, xMin, yMax, zMax))
                if (!hasEast && !packedSet.contains(packPos(px + 1, py + 1, pz)))
                    newEdgeShapes.add(createLineShape(xMax, yMax, zMin, xMax, yMax, zMax))
            }
            if (!hasDown) {
                if (!hasNorth && !packedSet.contains(packPos(px, py - 1, pz - 1)))
                    newEdgeShapes.add(createLineShape(xMin, yMin, zMin, xMax, yMin, zMin))
                if (!hasSouth && !packedSet.contains(packPos(px, py - 1, pz + 1)))
                    newEdgeShapes.add(createLineShape(xMin, yMin, zMax, xMax, yMin, zMax))
                if (!hasWest && !packedSet.contains(packPos(px - 1, py - 1, pz)))
                    newEdgeShapes.add(createLineShape(xMin, yMin, zMin, xMin, yMin, zMax))
                if (!hasEast && !packedSet.contains(packPos(px + 1, py - 1, pz)))
                    newEdgeShapes.add(createLineShape(xMax, yMin, zMin, xMax, yMin, zMax))
            }
            if (!hasNorth && !hasWest && !packedSet.contains(packPos(px - 1, py, pz - 1)))
                newEdgeShapes.add(createLineShape(xMin, yMin, zMin, xMin, yMax, zMin))
            if (!hasNorth && !hasEast && !packedSet.contains(packPos(px + 1, py, pz - 1)))
                newEdgeShapes.add(createLineShape(xMax, yMin, zMin, xMax, yMax, zMin))
            if (!hasSouth && !hasWest && !packedSet.contains(packPos(px - 1, py, pz + 1)))
                newEdgeShapes.add(createLineShape(xMin, yMin, zMax, xMin, yMax, zMax))
            if (!hasSouth && !hasEast && !packedSet.contains(packPos(px + 1, py, pz + 1)))
                newEdgeShapes.add(createLineShape(xMax, yMin, zMax, xMax, yMax, zMax))
        }

        activeEdgeShapes = newEdgeShapes
        activeFillShapes = newFills
    }

    const val PAD = 0.010
    const val THICK = 0.005
}