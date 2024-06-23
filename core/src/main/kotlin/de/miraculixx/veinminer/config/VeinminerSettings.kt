package de.miraculixx.veinminer.config

import kotlinx.serialization.Serializable

@Serializable
data class VeinminerSettings(
    var cooldown: Int = 20,
    var mustSneak: Boolean = false,
    var delay: Int = 1,
    var maxChain: Int = 100,
    var needCorrectTool: Boolean = true,
    var searchRadius: Int = 1,
)

