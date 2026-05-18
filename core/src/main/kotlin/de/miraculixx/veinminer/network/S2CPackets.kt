package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.resources.Identifier

@Serializable
data class ServerConfiguration(
    val outdated: Boolean,
    val settings: VeinminerSettings,
    val groups: List<BlockGroup<@Contextual Identifier>>,
    val veinBlocks: List<@Contextual Identifier>,
    val enchantmentActive: Boolean,
    val enchantmentKey: String?,
    val hostActive: Boolean,
    val hasUsePermission: Boolean,
)
