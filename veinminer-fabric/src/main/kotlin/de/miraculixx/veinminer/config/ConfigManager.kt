package de.miraculixx.veinminer.config

import kotlin.io.path.Path
import kotlin.io.path.writeText

object ConfigManager {
    private val blocksFile = Path("config/Veinminer/blocks.json")
    private val settingsFile = Path("config/Veinminer/settings.json")
    private val groupsFile = Path("config/Veinminer/groups.json")

    var veinBlocks: MutableSet<String> = loadBlocks()
        private set

    var settings: VeinminerSettings = loadSettings()
        private set

    var groups: MutableSet<BlockGroup<String>> = loadGroups()
        private set


//    init {
        // Kotlin Compiler analysis cannot statically determine if a var is initialized inside a function called from init
        // So we cant call reload from here to load them
//        settings = loadSettings()
//        veinBlocks = loadBlocks()
//        groups = loadGroups()
//    }

    fun reload() {
        settings = loadSettings()
        veinBlocks = loadBlocks()
        groups = loadGroups()
    }

    fun save() {
        settingsFile.writeText(json.encodeToString(settings))
        blocksFile.writeText(json.encodeToString(veinBlocks))
        groupsFile.writeText(json.encodeToString(groups))
    }

    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())
    private fun loadGroups() = groupsFile.load<MutableSet<BlockGroup<String>>>(mutableSetOf(BlockGroup("Ores", getDefaultOres())))
    private fun loadBlocks() = blocksFile.load<MutableSet<String>>(getDefaultOres())

    private fun getDefaultOres() = buildSet {
        setOf("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald").forEach {
            add("block.minecraft.${it}_ore")
            add("block.minecraft.deepslate_${it}_ore")
        }
        add("block.minecraft.nether_gold_ore")
        add("block.minecraft.nether_quartz_ore")
    }.toMutableSet()
}