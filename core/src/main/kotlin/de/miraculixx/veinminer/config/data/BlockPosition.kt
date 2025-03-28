package de.miraculixx.veinminer.config.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockPosition(
    val x: Int,
    val y: Int,
    val z: Int
)
