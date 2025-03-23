package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.extensions.load
import de.miraculixx.veinminer.config.utils.json
import org.bukkit.NamespacedKey
import kotlin.io.path.Path
import kotlin.io.path.writeText

object ConfigManager {
    private val blocksFile = Path("plugins/Veinminer/blocks.json")
    private val settingsFile = Path("plugins/Veinminer/settings.json")
    private val groupsFile = Path("plugins/Veinminer/groups.json")

    var veinBlocks: MutableSet<NamespacedKey> = loadBlocks()
        private set
    var settings: VeinminerSettings = loadSettings()
        private set
    var groups: MutableSet<BlockGroup<NamespacedKey>> = loadGroups()
        private set


    fun reload() {
        settings = loadSettings()
        veinBlocks = loadBlocks()
        groups = loadGroups()
    }

    fun save() {
        blocksFile.writeText(json.encodeToString(veinBlocks))
        settingsFile.writeText(json.encodeToString(settings))
        groupsFile.writeText(json.encodeToString(groups))
    }

    private fun loadBlocks() = blocksFile.load<MutableSet<String>>(mutableSetOf()).map { stringToNamespacedKey(it) }.toMutableSet()
    private fun loadGroups(): MutableSet<BlockGroup<NamespacedKey>> {
        val defaultSource = this::class.java.classLoader.getResourceAsStream("/default_groups.json")?.readAllBytes()?.decodeToString() ?: "[]"
        val defaultGroups = json.decodeFromString<MutableSet<BlockGroup<String>>>(defaultSource)
        return groupsFile.load<MutableSet<BlockGroup<String>>>(defaultGroups).map { it.toNamespacedKey() }.toMutableSet()
    }
    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())

    /**
     * Converts a string in the format "namespace:key" to a [NamespacedKey] and converts legacy material names to [NamespacedKey]
     */
    private fun stringToNamespacedKey(string: String): NamespacedKey {
        val split = string.split(":")
        return if (split.size != 2) NamespacedKey("minecraft", string.lowercase())
        else NamespacedKey(split[0], split[1])
    }

    private fun BlockGroup<String>.toNamespacedKey(): BlockGroup<NamespacedKey> {
        val newBlocks = blocks.map { stringToNamespacedKey(it) }.toMutableSet()
        val newTools = tools.map { stringToNamespacedKey(it) }.toMutableSet()
        return BlockGroup(name, newBlocks, newTools)
    }
}