package de.miraculixx.veinminer.config

import kotlinx.serialization.encodeToString
import org.bukkit.Material
import kotlin.io.path.Path
import kotlin.io.path.writeText

object ConfigManager {
    private val blocksFile = Path("plugins/Veinminer/blocks.json")
    private val settingsFile = Path("plugins/Veinminer/settings.json")
    private val groupsFile = Path("plugins/Veinminer/groups.json")

    var veinBlocks: MutableSet<Material>
        private set
    var settings: VeinminerSettings
        private set
    var groups: MutableSet<BlockGroup<Material>>
        private set

    init {
        settings = loadSettings()
        veinBlocks = loadBlocks()
        groups = loadGroups()
    }

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

    private fun loadBlocks() = blocksFile.load<MutableSet<Material>>(Material.entries.filter { it.name.endsWith("_ORE") }.toMutableSet())
    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())
    private fun loadGroups() = groupsFile.load<MutableSet<BlockGroup<Material>>>(
        mutableSetOf(
            BlockGroup("Ores", Material.entries.filter { it.name.endsWith("_ORE") }.toMutableSet()),
        )
    )
}