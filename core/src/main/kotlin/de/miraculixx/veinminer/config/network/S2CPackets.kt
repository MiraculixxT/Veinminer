package de.miraculixx.veinminer.config.network

import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminer.config.data.FixedBlockGroup
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfiguration(
    val cooldown: Int,
    val mustSneak: Boolean,
    val outdated: Boolean
)

@Serializable
data class BlockHighlighting(
    val allowed: Boolean,
    val icon: String,
    val blocks: List<BlockPosition>
)
