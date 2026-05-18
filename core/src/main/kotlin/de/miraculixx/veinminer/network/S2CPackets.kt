package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfiguration(
    val outdated: Boolean,
    val settings: VeinminerSettings,
    val groups: List<BlockGroup<String>>,
    val veinBlocks: List<String>,
    val enchantmentActive: Boolean,
    val enchantmentKey: String?,
    val hostActive: Boolean,
    val hasUsePermission: Boolean,
)
