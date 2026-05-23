package de.miraculixx.veinminerClient.config

import de.miraculixx.veinminer.extensions.load
import de.miraculixx.veinminer.pattern.DefaultPatterns
import de.miraculixx.veinminer.pattern.PatternConfig
import de.miraculixx.veinminer.pattern.PatternType
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.utils.json
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

@Serializable
data class ClientPatternSettings(
    val patterns: MutableList<PatternConfig> = DefaultPatterns.all(),
    var invertedScroll: Boolean = false,
)

object ClientPatternConfig {
    private val configFile: Path by lazy {
        Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve("Veinminer")
            .resolve("client_patterns.json")
    }

    var settings: ClientPatternSettings = ClientPatternSettings()
        private set

    fun load() {
        configFile.load(ClientPatternSettings(), json)
        ensureValid(save = true)
    }

    fun save() {
        ensureValid(save = false)
        write(settings)
    }

    fun enabledPatterns(): List<PatternConfig> {
        ensureValid(save = true)
        return settings.patterns.filter { it.enabled }
    }

    fun canRemove(pattern: PatternConfig): Boolean = settings.patterns.size > 1 && settings.patterns.contains(pattern)

    fun remove(pattern: PatternConfig): Boolean {
        if (!canRemove(pattern)) return false
        val removed = settings.patterns.remove(pattern)
        if (removed) save()
        return removed
    }

    fun canDisable(pattern: PatternConfig): Boolean {
        if (!pattern.enabled) return true
        return settings.patterns.count { it.enabled } > 1
    }

    fun add(type: PatternType): PatternConfig {
        val pattern = defaultFor(type)
        settings.patterns.add(pattern)
        save()
        return pattern
    }

    fun move(pattern: PatternConfig, delta: Int): Boolean {
        val from = settings.patterns.indexOf(pattern)
        if (from < 0) return false
        val to = (from + delta).coerceIn(0, settings.patterns.lastIndex)
        if (from == to) return false
        settings.patterns.removeAt(from)
        settings.patterns.add(to, pattern)
        save()
        return true
    }

    fun displayName(pattern: PatternConfig): String = when (pattern.type) {
        PatternType.NORMAL -> "Normal"
        PatternType.FLAT -> "Flat"
        PatternType.TUNNEL -> "Tunnel ${pattern.width.coerceAtLeast(1)}x${pattern.height.coerceAtLeast(1)}"
        PatternType.STAIRS -> "Stairs ${if (pattern.stairsUp) "Up" else "Down"} ${pattern.width.coerceAtLeast(1)}x${pattern.height.coerceAtLeast(1)}"
    }

    fun legacyShape(pattern: PatternConfig): Shape? = when (pattern.type) {
        PatternType.NORMAL -> Shape.NORMAL
        PatternType.FLAT -> Shape.FLAT
        PatternType.TUNNEL -> when (pattern.width.coerceAtLeast(1) to pattern.height.coerceAtLeast(1)) {
            1 to 1 -> Shape.TUNNEL_1X1
            2 to 2 -> Shape.TUNNEL_2X2
            3 to 3 -> Shape.TUNNEL_3X3
            else -> null
        }
        PatternType.STAIRS -> null
    }

    fun reset() {
        settings = ClientPatternSettings()
        save()
    }

    private fun ensureValid(save: Boolean) {
        var dirty = false
        val patterns = settings.patterns
        if (patterns.isEmpty()) {
            patterns.addAll(DefaultPatterns.all())
            dirty = true
        }
        patterns.forEach { pattern ->
            val width = pattern.width.coerceAtLeast(1)
            val height = pattern.height.coerceAtLeast(1)
            val color = pattern.color and 0xFFFFFF
            if (pattern.width != width) {
                pattern.width = width
                dirty = true
            }
            if (pattern.height != height) {
                pattern.height = height
                dirty = true
            }
            if (pattern.color != color) {
                pattern.color = color
                dirty = true
            }
        }
        if (patterns.none { it.enabled }) {
            patterns.first().enabled = true
            dirty = true
        }
        if (save && dirty) write(settings)
    }

    private fun write(settings: ClientPatternSettings) {
        configFile.createParentDirectories()
        configFile.writeText(json.encodeToString(settings))
    }

    fun defaultFor(type: PatternType): PatternConfig {
        val id = "${type.name.lowercase()}_${System.currentTimeMillis()}"
        return when (type) {
            PatternType.NORMAL -> PatternConfig(id = id, type = PatternType.NORMAL, color = 0x73D45C)
            PatternType.FLAT -> PatternConfig(id = id, type = PatternType.FLAT, color = 0x6FB7F0)
            PatternType.TUNNEL -> PatternConfig(id = id, type = PatternType.TUNNEL, color = 0xF09E4A, width = 3, height = 3)
            PatternType.STAIRS -> PatternConfig(id = id, type = PatternType.STAIRS, color = 0xB48CF0, width = 1, height = 2, stairsUp = true)
        }
    }
}
