package de.miraculixx.veinminer.config

import kotlinx.serialization.encodeToString
import org.bukkit.Material
import java.io.File

object ConfigManager {
    private val blocksFile = File("plugins/Veinminer/blocks.json")
    private val settingsFile = File("plugins/Veinminer/settings.json")

    val veinBlocks = blocksFile.loadFile<MutableSet<Material>>(Material.entries.filter { it.name.endsWith("_ORE") }.toMutableSet())
    val settings = settingsFile.loadFile<VeinminerSettings>(VeinminerSettings())

    fun save() {
        blocksFile.writeText(json.encodeToString(veinBlocks))
        settingsFile.writeText(json.encodeToString(settings))
    }
}