package de.miraculixx.veinminerClient.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.VeinminerClient
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import org.joml.Matrix4f
import java.util.OptionalDouble


object BlockHighlightingRenderer {
    val highlightedBlocks = mutableSetOf<BlockPosition>()

    private val renderDefault = RenderType.create(
        "${VeinminerClient.MOD_ID}_highlight",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.DEBUG_LINES,
        256,
        RenderType.CompositeState.builder()
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(RenderStateShard.LayeringStateShard.NO_LAYERING)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setCullState(RenderStateShard.CULL)
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .createCompositeState(false)
    )

    private val rendererTransparentOverlay = RenderType.create(
        "${VeinminerClient.MOD_ID}_highlight_transparent",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.DEBUG_LINES,
        256,
        RenderType.CompositeState.builder()
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(RenderStateShard.LayeringStateShard.NO_LAYERING)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
            .setCullState(RenderStateShard.CULL)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .createCompositeState(false)
    )

    fun render(context: WorldRenderContext) {
        val targetBlock = KeyBindManager.lastTarget
        if (highlightedBlocks.isEmpty() || targetBlock == null) return

        val client = VeinminerClient.client
        val stack = context.matrixStack() ?: return // Interfering render mod?
        val camPos = client.entityRenderDispatcher.camera.position
        println("Render: $targetBlock ($highlightedBlocks)")

        stack.pushPose()
        stack.translate(targetBlock.x - camPos.x, targetBlock.y - camPos.y, targetBlock.z - camPos.z)

        val matrix = stack.last().pose()
        val source = client.renderBuffers().bufferSource()

        // Default drawing
        val consumer = source.getBuffer(renderDefault)
        renderBlocks(consumer, matrix, highlightedBlocks, 255)
        source.endBatch(renderDefault)

        // Transparent drawing
        val bufferTransparent = source.getBuffer(rendererTransparentOverlay)
        renderBlocks(bufferTransparent, matrix, highlightedBlocks, 20)
        source.endBatch(rendererTransparentOverlay)

        stack.popPose()
    }

    private fun renderBlocks(buffer: VertexConsumer, matrix: Matrix4f, blocks: Set<BlockPosition>, transparency: Int) {
        blocks.forEach { blockPos ->
            val x = blockPos.x.toFloat()
            val y = blockPos.y.toFloat()
            val z = blockPos.z.toFloat()
            buffer.addVertex(matrix, x, y, z).setColor(255, 255, 255, transparency)
            buffer.addVertex(matrix, x + 1f, y + 1f, z + 1f).setColor(255, 255, 255, transparency)
        }
    }
}