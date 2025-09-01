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
    private val settingsFile = Path("plugins/Veinminer/settings.json")
    private val blocksFile = Path("plugins/Veinminer/blocks.json")
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

    var settings: VeinminerSettings = loadSettings()
        private set

    var veinBlocksRaw: MutableSet<String> = mutableSetOf()
        private set
    var veinBlocks: Set<NamespacedKey> = emptySet()
        private set

    var groupsRaw: MutableSet<BlockGroup<String>> = mutableSetOf()
        private set
    var groups: Set<BlockGroup<NamespacedKey>> = emptySet()
        private set


    fun reload() {
        settings = loadSettings()
        loadBlocks()
        loadGroups()
    }

    fun save() {
        blocksFile.writeText(json.encodeToString(veinBlocks))
        settingsFile.writeText(json.encodeToString(settings))
        groupsFile.writeText(json.encodeToString(groups))
    }

    private fun loadBlocks() {
        veinBlocksRaw = blocksFile.load<MutableSet<String>>(mutableSetOf(), json)
        veinBlocks = ConfigSerializer.parseList(veinBlocksRaw, ConfigSerializer.MaterialType.BLOCK)
    }

    private fun loadGroups() {
        // Load default group in case of missing file
        val defaultSource = this::class.java.classLoader.getResourceAsStream("default_groups.json")?.readAllBytes()?.decodeToString() ?: "[]"
        val defaultGroups = json.decodeFromString<MutableSet<BlockGroup<String>>>(defaultSource)

        groupsRaw = groupsFile.load<MutableSet<BlockGroup<String>>>(defaultGroups, json)
        groups = buildSet {
            groupsRaw.forEach { groupRaw ->
                BlockGroup(
                    groupRaw.name,
                    ConfigSerializer.parseList(groupRaw.blocks, ConfigSerializer.MaterialType.BLOCK).toMutableSet(),
                    ConfigSerializer.parseList(groupRaw.tools, ConfigSerializer.MaterialType.ITEM).toMutableSet()
                )
            }
        }
    }

    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())

}