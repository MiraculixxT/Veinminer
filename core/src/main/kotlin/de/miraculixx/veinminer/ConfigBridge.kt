package de.miraculixx.veinminer

import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings

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
