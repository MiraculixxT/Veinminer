package de.miraculixx.veinminer.config.data

import kotlinx.serialization.Serializable

@Serializable
data class VeinminerSettings(
    var cooldown: Int = 20,
    var mustSneak: Boolean = false,
    var delay: Int = 0,
    var maxChain: Int = 100,
    var needCorrectTool: Boolean = true,
    var searchRadius: Int = 1,
    var permissionRestricted: Boolean = false,
    var mergeItemDrops: Boolean = false,
    var autoUpdate: Boolean = false,
    var decreaseDurability: Boolean = true,
    var preventToolBreaking: Boolean = false,
    val client: VeinminerClientSettings = VeinminerClientSettings()
)

@Serializable
data class VeinminerClientSettings(
    var allow: Boolean = true,
    var translucentBlockHighlight: Boolean = true,
    var allBlocks: Boolean = false,
)
