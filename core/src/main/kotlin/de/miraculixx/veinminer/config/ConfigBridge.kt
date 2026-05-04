package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings

interface ConfigBridge {
    val settings: VeinminerSettings
    val veinBlocksRaw: MutableSet<String>
    val groupsRaw: MutableSet<BlockGroup<String>>
    fun reload(fromDisc: Boolean)
    fun save()
}

object ActiveConfig {
    @Volatile
    lateinit var bridge: ConfigBridge
}
