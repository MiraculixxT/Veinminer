package de.miraculixx.veinminer.config

import kotlinx.serialization.encodeToString
import java.io.File

object ConfigManager {
    private val blocksFile = File("config/Veinminer/blocks.json")
    private val settingsFile = File("config/Veinminer/settings.json")

    val veinBlocks = blocksFile.loadFile<MutableSet<String>>(mutableSetOf())
    val settings = settingsFile.loadFile<VeinminerSettings>(VeinminerSettings())

    fun save() {
        blocksFile.writeText(json.encodeToString(veinBlocks))
        settingsFile.writeText(json.encodeToString(settings))
    }
}