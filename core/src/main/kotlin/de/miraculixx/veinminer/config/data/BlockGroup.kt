package de.miraculixx.veinminer.config.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockGroup(
    var name: String,
    var blocks: MutableSet<String>,
    val tools: MutableSet<String> = mutableSetOf<String>()
)

@Serializable
data class FixedBlockGroup(
    val blocks: Set<String>,
    val tools: Set<String>
)