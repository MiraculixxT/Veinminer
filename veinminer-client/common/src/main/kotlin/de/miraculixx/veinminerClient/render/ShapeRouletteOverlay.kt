package de.miraculixx.veinminerClient.render

import de.miraculixx.veinminer.pattern.Shape
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import kotlin.math.abs
import kotlin.math.max

object ShapeRouletteOverlay {
    private const val FADE_DELAY_MS = 1500L
    private const val FADE_DURATION_MS = 500L
    private const val ANIM_SPEED = 0.30f
    private const val LINE_HEIGHT = 12
    private const val MARGIN_X = 6
    private const val MARGIN_Y = 6

    @Volatile
    private var cursor: Long = 0L

    @Volatile
    private var animated: Float = 0f

    @Volatile
    private var lastInteractionMs: Long = 0L

    /** Bump cursor by [delta] (signed, normally ±1 per wheel notch). */
    fun onScroll(delta: Int) {
        cursor += delta.toLong()
        lastInteractionMs = System.currentTimeMillis()
        playClick()
    }

    val currentShape: Shape
        get() {
            val size = Shape.entries.size
            val idx = ((cursor % size).toInt() + size) % size
            return Shape.entries[idx]
        }

    fun syncTo(shape: Shape) {
        cursor = shape.ordinal.toLong()
        animated = cursor.toFloat()
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
        //val yOffset = (frac * LINE_HEIGHT).toInt() // y bumping feels a bit sloppy
        val centerIdx = ((cursor % size).toInt() + size) % size

        for (rel in -1..1) {
            val idx = ((centerIdx + rel) % size + size) % size
            val name = Component.translatable("veinminer.shape.${entries[idx].name.lowercase()}")
            val baseY = MARGIN_Y + (rel + 1) * LINE_HEIGHT
            val distanceFromCenter = abs(rel - frac.toDouble())
            val rowAlpha = (alpha * (1.0 - 0.55 * distanceFromCenter.coerceAtMost(1.0))).toInt().coerceIn(0, alpha)
            val color = (rowAlpha shl 24) or 0xFFFFFF
            graphics.text(font, name, MARGIN_X, baseY, color)
        }
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
        return if (elapsed <= FADE_DELAY_MS) {
            255
        } else {
            val fade = (elapsed - FADE_DELAY_MS).toFloat() / FADE_DURATION_MS.toFloat()
            (255 * max(0f, 1f - fade)).toInt()
        }
    }

    private fun playClick() {
        val mc = Minecraft.getInstance()
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.4f))
    }
}
