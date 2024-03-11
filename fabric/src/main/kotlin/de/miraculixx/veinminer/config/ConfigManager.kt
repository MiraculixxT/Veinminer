package de.miraculixx.veinminer.config

import kotlinx.serialization.encodeToString
import java.io.File

object ConfigManager {
    private val blocksFile = File("config/Veinminer/blocks.json")
    private val settingsFile = File("config/Veinminer/settings.json")

    val veinBlocks = blocksFile.loadFile<MutableSet<String>>(buildSet {
        setOf("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald").forEach {
            add("block.minecraft.${it}_ore")
            add("block.minecraft.deepslate_${it}_ore")
        }
        add("block.minecraft.nether_gold_ore")
        add("block.minecraft.nether_quartz_ore")
    }.toMutableSet())
    val settings = settingsFile.loadFile<VeinminerSettings>(VeinminerSettings())

    fun save() {
        blocksFile.writeText(json.encodeToString(veinBlocks))
        settingsFile.writeText(json.encodeToString(settings))
    }
}