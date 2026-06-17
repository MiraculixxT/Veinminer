package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.ConfigBridge
import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import de.miraculixx.veinminer.extensions.load
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

abstract class BaseConfigManager<T>(
    configDir: Path,
    private val serializer: ConfigSerializer<T>,
    jsonModule: SerializersModule,
) : ConfigBridge {
    private val settingsFile = configDir.resolve("settings.json")
    private val blocksFile = configDir.resolve("blocks.json")
    private val groupsFile = configDir.resolve("groups.json")

    protected val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = jsonModule
    }

    val firstInstall: Boolean = !settingsFile.exists()

    final override var settings: VeinminerSettings = loadSettings()
        private set

    final override var veinBlocksRaw: MutableSet<String> = mutableSetOf()
        private set
    var networkVeinBlocks: List<String> = emptyList()
        private set
    var veinBlocks: Set<T> = emptySet()
        private set

    final override var groupsRaw: MutableSet<BlockGroup<String>> = mutableSetOf()
        private set
    var networkGroups: List<BlockGroup<String>> = emptyList()
        private set
    var groups: Set<BlockGroup<T>> = emptySet()
        private set

    /**
     * Reparses the raw config entries into native identifiers.
     * If `fromDisc` is true, it will also reload the raw data from the disc first.
     */
    final override fun reload(fromDisc: Boolean) {
        if (fromDisc) settings = loadSettings()
        if (loadBlocks(fromDisc)) saveBlocks()
        if (loadGroups(fromDisc)) saveGroups()
        refreshNetworkCache()
        onAfterReload()
    }

    /**
     * Hook for loader to broadcast updated configuration to all registered clients.
     */
    protected open fun onAfterReload() {}

    private fun refreshNetworkCache() {
        networkGroups = groups.map { group ->
            BlockGroup(
                name = group.name,
                blocks = group.blocks.mapTo(mutableSetOf()) { it.toString() },
                tools = group.tools.mapTo(mutableSetOf()) { it.toString() },
                override = group.override
            )
        }
        networkVeinBlocks = veinBlocks.map { it.toString() }
    }

    /**
     * Saves the current config to the disc and reparses the raw data into native identifiers.
     */
    final override fun save() {
        loadBlocks(false)
        loadGroups(false)

        saveSettings()
        saveBlocks()
        saveGroups()
        refreshNetworkCache()
        onAfterReload()
    }

    private fun loadBlocks(fromDisc: Boolean): Boolean {
        if (fromDisc) veinBlocksRaw = blocksFile.load<MutableSet<String>>(mutableSetOf(), json)
        val parsed = serializer.parseList(veinBlocksRaw, MaterialType.BLOCK)

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
                val parsedBlocks = serializer.parseList(groupRaw.blocks, MaterialType.BLOCK)
                val parsedTools = serializer.parseList(groupRaw.tools, MaterialType.ITEM)
                add(BlockGroup(groupRaw.name, parsedBlocks.parsed.toMutableSet(), parsedTools.parsed.toMutableSet(), groupRaw.override))

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
