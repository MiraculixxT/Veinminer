package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.extensions.load
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.resources.ResourceLocation
import kotlin.io.path.Path
import kotlin.io.path.writeText

object ConfigManager {
    private val settingsFile = Path("config/Veinminer/settings.json")
    private val blocksFile = Path("config/Veinminer/blocks.json")
    private val groupsFile = Path("config/Veinminer/groups.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(ResourceLocation::class, ResourceLocationSerializer)
        }
    }

    var settings: VeinminerSettings = loadSettings()
        private set

    var veinBlocksRaw: MutableSet<String> = mutableSetOf()
        private set
    var veinBlocks: Set<ResourceLocation> = emptySet()
        private set

    var groupsRaw: MutableSet<BlockGroup<String>> = mutableSetOf()
        private set
    var groups: Set<BlockGroup<ResourceLocation>> = emptySet()
        private set


    /**
     * Reparses the raw config entries into NamespacedKeys.
     * If `fromDisc` is true, it will also reload the raw data from the disc first
     */
    fun reload(fromDisc: Boolean) {
        if (fromDisc) settings = loadSettings()
        if (loadBlocks(fromDisc)) saveBlocks()
        if (loadGroups(fromDisc)) saveGroups()
    }

    /**
     * Saves the current config to the disc and reparses the raw data into NamespacedKeys
     */
    fun save() {
        // Parse raw data into NamespacedKeys
        loadBlocks(false)
        loadGroups(false)

        // Save to file
        saveSettings()
        saveBlocks()
        saveGroups()
    }

    /**
     * Loads the blocks from the config file and parses them into NamespacedKeys.
     * @return true if any invalid entries were found and removed, false otherwise
     */
    private fun loadBlocks(fromDisc: Boolean): Boolean {
        if (fromDisc) veinBlocksRaw = blocksFile.load<MutableSet<String>>(mutableSetOf(), json)
        val parsed = ConfigSerializer.parseList(veinBlocksRaw, ConfigSerializer.MaterialType.BLOCK)

        veinBlocks = parsed.parsed
        return if (parsed.invalid.isNotEmpty()) {
            veinBlocksRaw.removeAll(parsed.invalid)
            true
        } else false
    }

    private fun loadGroups(fromDisc: Boolean): Boolean {
        if (fromDisc) {
            val defaultSource = this::class.java.classLoader.getResourceAsStream("default_groups.json")?.readAllBytes()?.decodeToString() ?: "[]"
            val defaultGroups = json.decodeFromString<MutableSet<BlockGroup<String>>>(defaultSource)
            groupsRaw = groupsFile.load<MutableSet<BlockGroup<String>>>(defaultGroups, json)
        }

        var save = false
        groups = buildSet {
            groupsRaw.forEach { groupRaw ->
                val parsedBlocks = ConfigSerializer.parseList(groupRaw.blocks, ConfigSerializer.MaterialType.BLOCK)
                val parsedTools = ConfigSerializer.parseList(groupRaw.tools, ConfigSerializer.MaterialType.ITEM)
                add(BlockGroup(groupRaw.name, parsedBlocks.parsed.toMutableSet(), parsedTools.parsed.toMutableSet()))

                if (parsedBlocks.invalid.isNotEmpty() || parsedTools.invalid.isNotEmpty()) {
                    groupRaw.blocks.removeAll(parsedBlocks.invalid)
                    groupRaw.tools.removeAll(parsedTools.invalid)
                    save = true
                }
            }
        }
        return save
    }

    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())

    private fun saveBlocks() = blocksFile.writeText(json.encodeToString(veinBlocksRaw))
    private fun saveGroups() = groupsFile.writeText(json.encodeToString(groupsRaw))
    private fun saveSettings() = settingsFile.writeText(json.encodeToString(settings))
}