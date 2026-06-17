package de.miraculixx.veinminerClient.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.PoseStack
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


object BlockHighlightingRenderer {
    private var highlightingShape: VoxelShape = Shapes.empty()

    private val renderHighlightingTranslucent: RenderType by lazy {
        RenderType.create(
            "${ClientLifecycle.MOD_ID}:highlight_translucent",
            RenderSetup.builder(
                RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "pipeline/highlight_translucent"))
                        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .withCull(false)
                        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
                        .build()
                )
            ).createRenderSetup()
        )
    }

    fun render(stack: PoseStack, collector: OrderedSubmitNodeCollector, camPos: Vec3) {
        val targetBlock = KeyBindManager.lastTarget
        if (highlightingShape.isEmpty || targetBlock == null) return

        stack.pushPose()
        try {
            stack.translate(targetBlock.x - camPos.x, targetBlock.y - camPos.y, targetBlock.z - camPos.z)

            collector.submitShapeOutline(stack, highlightingShape, RenderTypes.lines(), ARGB.white(255), 1.0f, false)
            if (NetworkManager.settings.client.translucentBlockHighlight) {
                collector.submitShapeOutline(stack, highlightingShape, renderHighlightingTranslucent, ARGB.white(50), 1.0f, true)
            }
        } finally {
            stack.popPose()
        }
    }

    fun setShape(positions: List<BlockPosition>) {
        val source = KeyBindManager.lastTarget
        if (positions.isEmpty() || source == null) {
            highlightingShape = Shapes.empty()
            return
        }

        val splines = positions.map {
            val box = Shapes.box(-0.010, -0.010, -0.010, 1.010, 1.010, 1.010) // Outline
            //val box = Shapes.box(0.35, 0.35, 0.35, 0.65, 0.65, 0.65) // Inline Box (more clutter)
            val dx = it.x - source.x
            val dy = it.y - source.y
            val dz = it.z - source.z
            if (dx == 0 && dy == 0 && dz == 0) box
            else box.move(dx.toDouble(), dy.toDouble(), dz.toDouble())
        }
        highlightingShape = Shapes.or(splines.first(), *splines.toTypedArray())
    }
}
