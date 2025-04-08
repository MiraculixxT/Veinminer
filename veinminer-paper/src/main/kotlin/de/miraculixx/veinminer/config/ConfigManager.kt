package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.extensions.load
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.bukkit.NamespacedKey
import kotlin.io.path.Path
import kotlin.io.path.writeText

object ConfigManager {
    private val blocksFile = Path("plugins/Veinminer/blocks.json")
    private val settingsFile = Path("plugins/Veinminer/settings.json")
    private val groupsFile = Path("plugins/Veinminer/groups.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(NamespacedKey::class, NamespacedKeySerializer)
        }
    }

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

    private fun loadBlocks() = blocksFile.load<MutableSet<NamespacedKey>>(mutableSetOf(), json)
    private fun loadGroups(): MutableSet<BlockGroup<NamespacedKey>> {
        val defaultSource = this::class.java.classLoader.getResourceAsStream("default_groups.json")?.readAllBytes()?.decodeToString() ?: "[]"
        val defaultGroups = json.decodeFromString<MutableSet<BlockGroup<NamespacedKey>>>(defaultSource)
        return groupsFile.load<MutableSet<BlockGroup<NamespacedKey>>>(defaultGroups, json)
    }

    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())

}