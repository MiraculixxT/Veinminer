package de.miraculixx.veinminerClient.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import java.util.*


object BlockHighlightingRenderer {
    private var highlightingShape: VoxelShape = Shapes.empty()

    private val renderHighlighting: RenderType by lazy<RenderType> { RenderType.lines() }
    private val renderHighlightingTranslucent: RenderType by lazy<RenderType> {
        RenderType.create(
            "${ClientLifecycle.MOD_ID}:highlight_translucent",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                .setTextureState(RenderStateShard.NO_TEXTURE)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setLineState(RenderStateShard.DEFAULT_LINE)
                .createCompositeState(false)
        ) as RenderType
    }


    // OrderedSubmitNodeCollector
    fun render(stack: PoseStack,
               source: MultiBufferSource.BufferSource,
               camPos: Vec3,
               isTranslucentPass: Boolean) {
        val targetBlock = KeyBindManager.lastTarget
        if (highlightingShape.isEmpty || targetBlock == null) return

        stack.pushPose()
        stack.translate(targetBlock.x - camPos.x, targetBlock.y - camPos.y, targetBlock.z - camPos.z)

        val matrix = stack.last().pose()

        val drawTranslucent = NetworkManager.settings.client.translucentBlockHighlight

        if (!isTranslucentPass) {
            renderBlocks(source, renderHighlighting, matrix, highlightingShape, 255)
        } else if (drawTranslucent) {
            renderBlocks(source, renderHighlightingTranslucent, matrix, highlightingShape, 50)
        }

        if (!isTranslucentPass) {
            source.endBatch(renderHighlighting)
        } else if (drawTranslucent) {
            source.endBatch(renderHighlightingTranslucent)
        }

        stack.popPose()
    }

    private fun renderBlocks(source: MultiBufferSource.BufferSource, renderer: RenderType, matrix: Matrix4f, shape: VoxelShape, transparency: Int) {
        val buffer = source.getBuffer(renderer)
        shape.forAllEdges { x, y, z, dx, dy, dz ->
            val x = x.toFloat()
            val y = y.toFloat()
            val z = z.toFloat()
            val dx = dx.toFloat()
            val dy = dy.toFloat()
            val dz = dz.toFloat()
            val relX = dx - x
            val relY = dy - y
            val relZ = dz - z

            // Outline
            buffer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, transparency)
                .setNormal(relX, relY, relZ)
            buffer.addVertex(matrix, dx, dy, dz)
                .setColor(255, 255, 255, transparency)
                .setNormal(relX, relY, relZ)
        }
        source.endLastBatch()
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
