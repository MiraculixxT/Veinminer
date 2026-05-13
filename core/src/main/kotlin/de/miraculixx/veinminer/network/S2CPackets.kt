package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.data.BlockPosition
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfiguration(
    val cooldown: Int,
    val mustSneak: Boolean,
    val outdated: Boolean,
    val translucentBlockHighlight: Boolean,
)

@Serializable
data class BlockHighlighting(
    val allowed: Boolean,
    val icon: String,
    val blocks: List<BlockPosition>
)
