package de.miraculixx.veinminerClient.render

import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.pattern.Shape
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import kotlin.math.abs
import kotlin.math.max

object ShapeRouletteOverlay {
    private const val FADE_DELAY_MS = 5000L
    private const val FADE_DURATION_MS = 500L
    private const val ANIM_SPEED = 0.30f
    private const val LINE_HEIGHT = 12
    private const val MARGIN_X = 6
    private const val MARGIN_Y = 6
    private const val DEFAULT_DEPTH = 6

    /** Selectable depth values shown in the bar. Last entry = unlimited. */
    private val DEPTH_VALUES: List<Int> = (2..10).toList() + KeyPress.UNLIMITED_DEPTH

    @Volatile
    private var cursor: Long = 0L

    @Volatile
    private var animated: Float = 0f

    @Volatile
    private var depthIndex: Int = DEPTH_VALUES.indexOf(DEFAULT_DEPTH)

    @Volatile
    private var lastInteractionMs: Long = 0L

    fun onScroll(delta: Int) {
        cursor += delta.toLong()
        bumpInteraction()
        playClick(pitch = 1.6f)
    }

    fun onDepthScroll(delta: Int) {
        val next = (depthIndex + delta).coerceIn(0, DEPTH_VALUES.lastIndex)
        if (next == depthIndex) return // edge of range; no audible click for muted change
        depthIndex = next
        bumpInteraction()
        playClick(pitch = 1.2f)
    }

    val currentShape: Shape
        get() {
            val size = Shape.entries.size
            val idx = ((cursor % size).toInt() + size) % size
            return Shape.entries[idx]
        }

    val currentDepth: Int get() = DEPTH_VALUES[depthIndex]

    fun syncTo(shape: Shape, depth: Int) {
        cursor = shape.ordinal.toLong()
        animated = cursor.toFloat()
        val idx = DEPTH_VALUES.indexOf(depth)
        if (idx >= 0) depthIndex = idx
    }

    fun render(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val elapsed = System.currentTimeMillis() - lastInteractionMs
        if (lastInteractionMs == 0L || elapsed > FADE_DELAY_MS + FADE_DURATION_MS) return

        advance(deltaTracker.realtimeDeltaTicks)

        val alpha = computeAlpha(elapsed)
        if (alpha <= 0) return

        val entries = Shape.entries
        val size = entries.size
        val font = Minecraft.getInstance().font

        val frac = animated - cursor.toFloat()
        val centerIdx = ((cursor % size).toInt() + size) % size

        for (rel in -1..1) {
            val idx = ((centerIdx + rel) % size + size) % size
            val name = Component.translatable("veinminer.shape.${entries[idx].name.lowercase()}")
            val baseY = MARGIN_Y + (rel + 1) * LINE_HEIGHT + LINE_HEIGHT
            val distanceFromCenter = abs(rel - frac.toDouble())
            val rowAlpha = (alpha * (1.0 - 0.55 * distanceFromCenter.coerceAtMost(1.0))).toInt().coerceIn(0, alpha)
            val color = (rowAlpha shl 24) or 0xFFFFFF
            graphics.text(font, name, MARGIN_X, baseY, color)
        }

        renderDepthBar(graphics, font, alpha)
    }

    private fun renderDepthBar(graphics: GuiGraphicsExtractor, font: Font, alpha: Int) {
        val baseY = MARGIN_Y
        val filled = depthIndex + 1
        val cells = buildString {
            for (i in DEPTH_VALUES.indices) append(if (i < filled) '■' else '□')
        }
        val value = if (depthIndex == DEPTH_VALUES.lastIndex) "∞" else currentDepth.toString()
        val label = Component.translatable("veinminer.shape.depth", cells, value)
        val color = (alpha shl 24) or 0xFFFFFF
        graphics.text(font, label, MARGIN_X, baseY, color)
    }

    private fun advance(deltaTicks: Float) {
        val diff = cursor.toFloat() - animated
        if (abs(diff) < 0.001f) {
            animated = cursor.toFloat()
            return
        }
        animated += diff * (ANIM_SPEED * deltaTicks).coerceAtMost(1f)
    }

    private fun computeAlpha(elapsed: Long): Int {
        return if (elapsed <= FADE_DELAY_MS) 255
        else {
            val fade = (elapsed - FADE_DELAY_MS).toFloat() / FADE_DURATION_MS.toFloat()
            (255 * max(0f, 1f - fade)).toInt()
        }
    }

    private fun bumpInteraction() {
        lastInteractionMs = System.currentTimeMillis()
    }

    private fun playClick(pitch: Float) {
        val mc = Minecraft.getInstance()
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.4f))
    }
}
