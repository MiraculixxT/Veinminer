package de.miraculixx.veinminer.config

import kotlinx.serialization.Serializable

@Serializable
data class BlockGroup<T>(
    var name: String,
    var blocks: MutableSet<T>
)