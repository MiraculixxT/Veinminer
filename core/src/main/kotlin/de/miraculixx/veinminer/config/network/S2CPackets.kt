package de.miraculixx.veinminer.config.network

import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminer.config.data.FixedBlockGroup
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfiguration(
    val globalBlockList: Set<String>,
    val blockGroups: Set<FixedBlockGroup>,
    val cooldown: Int,
    val mustSneak: Boolean,
    val outdated: Boolean
)

@Serializable
data class BlockHighlighting(
    val allowed: Boolean,
    val blocks: List<BlockPosition>
)
