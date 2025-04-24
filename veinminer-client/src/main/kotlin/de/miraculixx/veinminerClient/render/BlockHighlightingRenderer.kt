package de.miraculixx.veinminerClient.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.LogicOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.VeinminerClient
import de.miraculixx.veinminerClient.network.NetworkManager
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import java.util.*
import kotlin.text.toFloat

// TODO: Check for optimization, Copilot threw this at me
object BlockHighlightingRenderer {
    private var highlightingShape: VoxelShape = Shapes.empty()

    private val renderDefault
        get() = RenderType.create(
                "${VeinminerClient.MOD_ID}_highlight",
                256,
                RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withColorLogic(LogicOp.NONE)
                    .withColorWrite(true)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                    .withDepthWrite(true)
                    .withLocation("pipeline/wireframe")
                    .build(),
                RenderType.CompositeState.builder()
                    .setLineState(RenderStateShard.DEFAULT_LINE)
                    .createCompositeState(RenderType.OutlineProperty.IS_OUTLINE)
            )

    private val renderDefaultTranslucent
        get() = RenderType.create(
            "${VeinminerClient.MOD_ID}_highlight_translucent",
            256,
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                .withColorLogic(LogicOp.NONE)
                .withColorWrite(true)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                .withDepthWrite(false)
                .withLocation("pipeline/wireframe")
                .build(),
            RenderType.CompositeState.builder()
                .setLineState(RenderStateShard.DEFAULT_LINE)
                .createCompositeState(RenderType.OutlineProperty.NONE)
        )

//    private val renderDefault = RenderType.create(
//        "${VeinminerClient.MOD_ID}_highlight",
//        DefaultVertexFormat.POSITION_COLOR,
//        VertexFormat.Mode.DEBUG_LINES,
//        256,
//        RenderType.CompositeState.builder()
//            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
//            .setLayeringState(RenderStateShard.LayeringStateShard.NO_LAYERING)
//            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
//            .setCullState(RenderStateShard.CULL)
//            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
//            .createCompositeState(false)
//    )

//    private val rendererTransparentOverlay = RenderType.create(
//        "${VeinminerClient.MOD_ID}_highlight_transparent",
//        DefaultVertexFormat.POSITION_COLOR,
//        VertexFormat.Mode.DEBUG_LINES,
//        256,
//        RenderType.CompositeState.builder()
//            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
//            .setLayeringState(RenderStateShard.LayeringStateShard.NO_LAYERING)
//            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
//            .setCullState(RenderStateShard.CULL)
//            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
//            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
//            .createCompositeState(false)
//    )

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
        val consumer = source.getBuffer(renderDefault)
        renderBlocks(consumer, matrix, highlightingShape, 255)
        source.endBatch(renderDefault)

        // Translucent drawing
        if (NetworkManager.translucentBlockHighlight) {
            val bufferTransparent = source.getBuffer(renderDefaultTranslucent)
            renderBlocks(bufferTransparent, matrix, highlightingShape, 20)
            source.endBatch(renderDefaultTranslucent)
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
            //val box = Shapes.box(0.35, 0.35, 0.35, 0.65, 0.65, 0.65) // Inline Box
            val dx = it.x - source.x
            val dy = it.y - source.y
            val dz = it.z - source.z
            if (dx == 0 && dy == 0 && dz == 0) box
            else box.move(dx.toDouble(), dy.toDouble(), dz.toDouble())
        }
        highlightingShape = Shapes.or(splines.first(), *splines.toTypedArray())
    }
}