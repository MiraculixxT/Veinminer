package de.miraculixx.veinminer.config.network

import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminer.config.pattern.Pattern
import de.miraculixx.veinminer.config.pattern.Surface
import kotlinx.serialization.Serializable

@Serializable
data class JoinInformation(
    val veinminerClientVersion: String,
)

@Serializable
data class RequestBlockVein(
    val blockPosition: BlockPosition,
    val surface: Surface,
    val pattern: Pattern
)

@Serializable
data class KeyPress(
    val pressed: Boolean
)
