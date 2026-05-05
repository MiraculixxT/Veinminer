package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.pattern.Pattern
import de.miraculixx.veinminer.pattern.Surface
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
