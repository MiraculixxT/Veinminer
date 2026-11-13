package de.miraculixx.veinminerClient.config

import de.miraculixx.veinminer.extensions.load
import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.pattern.DefaultPatterns
import de.miraculixx.veinminer.pattern.PatternConfig
import de.miraculixx.veinminer.pattern.PatternType
import de.miraculixx.veinminer.utils.json
import kotlinx.serialization.Serializable
import java.nio.file.Path
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

@Serializable
data class ClientPatternSettings(
    val patterns: MutableList<PatternConfig> = DefaultPatterns.all(),
    var invertedScroll: Boolean = false,
    var lockPatternScroll: Boolean = false,
    var showShortcutHint: Boolean = true,
    var selectedPatternId: String? = null,
    var selectedDepth: Int = KeyPress.UNLIMITED_DEPTH,
    val names: MutableMap<String, String> = mutableMapOf(),
    val icons: MutableMap<String, String> = mutableMapOf(),
)

object ClientPatternConfig {
    private var configFile = Path.of("config", "Veinminer", "client_patterns.json")

    var settings: ClientPatternSettings = ClientPatternSettings()
        private set

    fun configure(configDir: Path) {
        configFile = configDir.resolve("Veinminer").resolve("client_patterns.json")
    }

    fun load() {
        settings = configFile.load(ClientPatternSettings(), json)
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

    fun selectedPattern(): PatternConfig {
        val enabled = enabledPatterns()
        return enabled.firstOrNull { it.id == settings.selectedPatternId } ?: enabled.first()
    }

    fun setSelection(pattern: PatternConfig, depth: Int) {
        if (settings.selectedPatternId == pattern.id && settings.selectedDepth == depth) return
        settings.selectedPatternId = pattern.id
        settings.selectedDepth = depth
        save()
    }

    fun canRemove(pattern: PatternConfig): Boolean = settings.patterns.size > 1 && settings.patterns.contains(pattern)

    fun remove(pattern: PatternConfig): Boolean {
        if (!canRemove(pattern)) return false
        val removed = settings.patterns.remove(pattern)
        if (removed) {
            settings.names.remove(pattern.id)
            settings.icons.remove(pattern.id)
            save()
        }
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

    const val MAX_NAME_LENGTH = 32
    private val iconStacks = HashMap<String, ItemStack>()

    fun parseItem(raw: String): Item? {
        if (raw.isBlank()) return null
        val id = Identifier.tryParse(raw.trim().lowercase()) ?: return null
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null)?.takeIf { it !== Items.AIR }
    }

    fun iconStack(pattern: PatternConfig): ItemStack? = settings.icons[pattern.id]
        ?.let { raw -> iconStacks.getOrPut(raw) { parseItem(raw)?.let(::ItemStack) ?: ItemStack.EMPTY } }
        ?.takeUnless { it.isEmpty }

    /** Invalid or blank input removes the custom icon (default shape icon) */
    fun setIcon(pattern: PatternConfig, raw: String): Boolean {
        val item = parseItem(raw)
        if (item == null) settings.icons.remove(pattern.id)
        else settings.icons[pattern.id] = BuiltInRegistries.ITEM.getKey(item).toString()
        save()
        return item != null
    }

    fun displayName(pattern: PatternConfig): String = settings.names[pattern.id] ?: defaultName(pattern)

    /** Blank or default name removes the custom name */
    fun rename(pattern: PatternConfig, name: String) {
        val clean = name.trim().take(MAX_NAME_LENGTH)
        if (clean.isEmpty() || clean == defaultName(pattern)) settings.names.remove(pattern.id)
        else settings.names[pattern.id] = clean
        save()
    }

    fun defaultName(pattern: PatternConfig): String = when (pattern.type) {
        PatternType.NORMAL -> "Normal"
        PatternType.FLAT -> "Flat"
        PatternType.TUNNEL -> "Tunnel ${pattern.width.coerceAtLeast(1)}x${pattern.height.coerceAtLeast(1)}"
        PatternType.STAIRS -> "Stairs ${if (pattern.stairsUp) "Up" else "Down"} ${pattern.width.coerceAtLeast(1)}x${pattern.height.coerceAtLeast(1)}"
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
        val selectedId = settings.selectedPatternId
        if (selectedId != null && patterns.none { it.enabled && it.id == selectedId }) {
            settings.selectedPatternId = null
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
