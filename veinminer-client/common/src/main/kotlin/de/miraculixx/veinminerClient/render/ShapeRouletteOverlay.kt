package de.miraculixx.veinminerClient.render

import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.pattern.Shape
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import kotlin.math.abs
import kotlin.math.max

object ShapeRouletteOverlay {
    private const val FADE_DELAY_MS = 5000L
    private const val FADE_DURATION_MS = 500L
    private const val ANIM_SPEED = 0.30f
    private const val PANEL_X = 4
    private const val PANEL_Y = 4
    private const val PANEL_WIDTH = 140
    private const val PAD_X = 6
    private const val PAD_Y = 6
    private const val ROW_HEIGHT = 18
    private const val ICON_SIZE = 16
    private const val ICON_TEX_SIZE = 16
    private const val BAR_CELL_W = 8
    private const val BAR_CELL_H = 4
    private const val BAR_GAP = 1
    private const val DEFAULT_DEPTH = 6

    /** Selectable depth values shown in the bar. Last entry = unlimited. */
    private val DEPTH_VALUES: List<Int> = (2..10).toList() + KeyPress.UNLIMITED_DEPTH

    @Volatile private var cursor: Long = 0L
    @Volatile private var animated: Float = 0f
    @Volatile private var depthIndex: Int = DEPTH_VALUES.indexOf(DEFAULT_DEPTH)
    @Volatile private var lastInteractionMs: Long = 0L

    fun onScroll(delta: Int) {
        cursor += delta.toLong()
        bumpInteraction()
        playClick(1.6f)
    }

    fun onDepthScroll(delta: Int) {
        val next = (depthIndex + delta).coerceIn(0, DEPTH_VALUES.lastIndex)
        if (next == depthIndex) return
        depthIndex = next
        bumpInteraction()
        playClick(0.8f + 0.4f * (depthIndex.toFloat() / DEPTH_VALUES.size))
    }

    val currentShape: Shape
        get() {
            val size = Shape.entries.size
            return Shape.entries[((cursor % size).toInt() + size) % size]
        }

    val currentDepth: Int get() = DEPTH_VALUES[depthIndex]

    fun syncTo(shape: Shape, depth: Int) {
        cursor = shape.ordinal.toLong()
        animated = cursor.toFloat()
        val idx = DEPTH_VALUES.indexOf(depth)
        if (idx >= 0) depthIndex = idx
    }

    fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        val elapsed = System.currentTimeMillis() - lastInteractionMs
        if (lastInteractionMs == 0L || elapsed > FADE_DELAY_MS + FADE_DURATION_MS) return
        advance(deltaTracker.realtimeDeltaTicks)
        val alpha = computeAlpha(elapsed)
        if (alpha <= 0) return

        val font = Minecraft.getInstance().font
        val entries = Shape.entries
        val size = entries.size
        val centerIdx = ((cursor % size).toInt() + size) % size

        val panelH = PAD_Y * 2 + 3 * ROW_HEIGHT + 4 + BAR_CELL_H + 4
        val x1 = PANEL_X
        val y1 = PANEL_Y
        val x2 = PANEL_X + PANEL_WIDTH
        val y2 = PANEL_Y + panelH

        drawPanel(graphics, x1, y1, x2, y2, alpha)
        drawCarousel(graphics, font, entries, size, centerIdx, x1, x2, y1, alpha)
        drawDepthBar(graphics, font, entries[centerIdx], x1, y1, alpha)
    }

    private fun drawPanel(g: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, alpha: Int) {
        g.fill(x1, y1, x2, y2, mixAlpha(alpha, 0xC0, 0x101418))
        g.fill(x1, y1, x2, y1 + 1, mixAlpha(alpha, 0x60, 0xFFFFFF))     // top highlight
        g.fill(x1, y2 - 1, x2, y2, mixAlpha(alpha, 0x60, 0x000000))     // bottom shadow
        g.fill(x1, y1, x1 + 1, y2, mixAlpha(alpha, 0x40, 0xFFFFFF))     // left highlight
        g.fill(x2 - 1, y1, x2, y2, mixAlpha(alpha, 0x40, 0x000000))     // right shadow
    }

    private fun drawCarousel(
        g: GuiGraphics,
        font: Font,
        entries: List<Shape>,
        size: Int,
        centerIdx: Int,
        x1: Int,
        x2: Int,
        y1: Int,
        alpha: Int,
    ) {
        for (rel in -1..1) {
            val idx = ((centerIdx + rel) % size + size) % size
            val shape = entries[idx]
            val rowY = y1 + PAD_Y + (rel + 1) * ROW_HEIGHT
            val accent = colorFor(shape)
            if (rel == 0) { // highlight box
                // selected row tint + left accent bar
                g.fill(x1 + 2, rowY - 1, x2 - 2, rowY + ROW_HEIGHT - 1, mixAlpha(alpha, 0x35, accent))
                g.fill(x1 + 2, rowY - 1, x1 + 4, rowY + ROW_HEIGHT - 1, mixAlpha(alpha, 0xE0, accent))
            }
            val iconX = x1 + PAD_X + 4
            val iconY = rowY + (ROW_HEIGHT - ICON_SIZE) / 2 - 1
            val rowAlpha = if (rel == 0) alpha else (alpha * 0.55).toInt().coerceAtLeast(0)
            val iconTint = if (rel == 0) mixAlpha(rowAlpha, 0xFF, accent) else mixAlpha(rowAlpha, 0xFF, 0xFFFFFF)
            g.blit(
                RenderPipelines.GUI_TEXTURED, iconFor(shape),
                iconX, iconY, 0f, 0f,
                ICON_SIZE, ICON_SIZE, ICON_TEX_SIZE, ICON_TEX_SIZE,
                iconTint
            )
            val textX = iconX + ICON_SIZE + 5
            val textY = rowY + (ROW_HEIGHT - 8) / 2
            val rgb = if (rel == 0) accent else 0xC8C8C8
            g.drawString(font, Component.translatable("veinminer.shape.${shape.name.lowercase()}"), textX, textY, mixAlpha(rowAlpha, 0xFF, rgb))
        }
    }

    private fun drawDepthBar(g: GuiGraphics, font: Font, selectedShape: Shape, x1: Int, y1: Int, alpha: Int) {
        val barX = x1 + PAD_X + 4
        val barY = y1 + PAD_Y + 3 * ROW_HEIGHT + 4
        val accent = colorFor(selectedShape)
        val filled = depthIndex + 1
        for (i in DEPTH_VALUES.indices) {
            val cellX = barX + i * (BAR_CELL_W + BAR_GAP)
            val on = i < filled
            val color = if (on) mixAlpha(alpha, 0xFF, accent) else mixAlpha(alpha, 0x70, 0x383838)
            g.fill(cellX, barY, cellX + BAR_CELL_W, barY + BAR_CELL_H, color)
        }
        val labelX = barX + DEPTH_VALUES.size * (BAR_CELL_W + BAR_GAP) + 4
        val labelY = barY - 2
        val label = if (depthIndex == DEPTH_VALUES.lastIndex) "∞" else currentDepth.toString()
        g.drawString(font, Component.literal(label), labelX, labelY, mixAlpha(alpha, 0xFF, 0xFFFFFF))
    }

    /** Per-shape accent color (RGB) */
    private fun colorFor(shape: Shape): Int = when (shape) {
        Shape.NORMAL -> 0x73D45C        // green
        Shape.TUNNEL_1X1 -> 0xF0C56E    // orange (light)
        Shape.TUNNEL_2X2 -> 0xF09E4A    // orange (mid)
        Shape.TUNNEL_3X3 -> 0xE87434    // orange (dark)
        Shape.FLAT -> 0x6FB7F0          // blue
    }

    private fun iconFor(shape: Shape): Identifier =
        Identifier.fromNamespaceAndPath(
            "veinminer_client",
            "textures/gui/sprite/shape/${shape.name.lowercase()}.png"
        )

    private fun mixAlpha(alpha: Int, alphaFrac: Int, rgb: Int): Int {
        val a = (alpha * alphaFrac / 255).coerceIn(0, 255)
        return (a shl 24) or (rgb and 0xFFFFFF)
    }

    private fun advance(deltaTicks: Float) {
        val diff = cursor.toFloat() - animated
        if (abs(diff) < 0.001f) {
            animated = cursor.toFloat()
            return
        }
        animated += diff * (ANIM_SPEED * deltaTicks).coerceAtMost(1f)
    }

    private fun computeAlpha(elapsed: Long): Int =
        if (elapsed <= FADE_DELAY_MS) 255
        else (255 * max(0f, 1f - (elapsed - FADE_DELAY_MS).toFloat() / FADE_DURATION_MS.toFloat())).toInt()

    private fun bumpInteraction() {
        lastInteractionMs = System.currentTimeMillis()
    }

    private fun playClick(pitch: Float) {
        val mc = Minecraft.getInstance()
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.4f))
    }
}
