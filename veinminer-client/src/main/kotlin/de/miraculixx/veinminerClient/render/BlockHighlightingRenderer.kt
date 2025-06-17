package de.miraculixx.veinminerClient.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.VeinminerClient
import de.miraculixx.veinminerClient.network.NetworkManager
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import java.util.*


object BlockHighlightingRenderer {
    private var highlightingShape: VoxelShape = Shapes.empty()

    private val renderHighlighting
        get() = RenderType.create(
            "${VeinminerClient.MOD_ID}:highlight",
            512,
            RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("veinminer-client", "pipeline/highlight"))
                    .withDepthWrite(false)
                    .withCull(false)
                    .withColorWrite(true, true)
                    .build()
            ),
            RenderType.CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(1.0)))
                .setLayeringState(RenderStateShard.LayeringStateShard.NO_LAYERING)
                .createCompositeState(false)
        )

    private val renderHighlightingTranslucent
        get() = RenderType.create(
            "${VeinminerClient.MOD_ID}:highlight_translucent",
            512,
            RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("veinminer-client", "pipeline/highlight_translucent"))
                    .withDepthWrite(true)
                    .withCull(false)
                    .withColorWrite(true, true)
                    .build()
            ),
            RenderType.CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(1.0)))
                .setLayeringState(RenderStateShard.LayeringStateShard.NO_LAYERING)
                .createCompositeState(false)
        )


    fun render(context: WorldRenderContext) {
        val targetBlock = KeyBindManager.lastTarget
        if (highlightingShape.isEmpty || targetBlock == null) return

        val client = VeinminerClient.client
        val stack = context.matrixStack() ?: return // Interfering render mod?
        val camPos = client.entityRenderDispatcher.camera.position

        stack.pushPose()
        stack.translate(targetBlock.x - camPos.x, targetBlock.y - camPos.y, targetBlock.z - camPos.z)

        val matrix = stack.last().pose()
        val source = client.renderBuffers().bufferSource()

        // Default drawing
        val consumer = source.getBuffer(renderHighlighting)
        renderBlocks(consumer, matrix, highlightingShape, 255)
        source.endBatch(renderHighlighting)

        // Translucent drawing
        if (NetworkManager.translucentBlockHighlight) {
            val bufferTransparent = source.getBuffer(renderHighlightingTranslucent)
            renderBlocks(bufferTransparent, matrix, highlightingShape, 20)
            source.endBatch(renderHighlightingTranslucent)
        }

        stack.popPose()
    }

    private fun renderBlocks(buffer: VertexConsumer, matrix: Matrix4f, shape: VoxelShape, transparency: Int) {
        shape.forAllEdges { x, y, z, dx, dy, dz ->
            // Outline
            buffer.addVertex(matrix, x.toFloat(), y.toFloat(), z.toFloat()).setColor(255, 255, 255, transparency)
            buffer.addVertex(matrix, dx.toFloat(), dy.toFloat(), dz.toFloat()).setColor(255, 255, 255, transparency)
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