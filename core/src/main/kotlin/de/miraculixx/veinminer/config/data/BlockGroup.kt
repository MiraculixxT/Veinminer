package de.miraculixx.veinminer.config.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockGroup<T>(
    var name: String,
    var blocks: MutableSet<T>,
    val tools: MutableSet<T> = mutableSetOf<T>(),
    val override: VeinminerSettingsOverride = VeinminerSettingsOverride()
)

@Serializable
data class FixedBlockGroup<T>(
    val blocks: Set<T>,
    val tools: Set<T>,
    val override: VeinminerSettingsOverride?
)