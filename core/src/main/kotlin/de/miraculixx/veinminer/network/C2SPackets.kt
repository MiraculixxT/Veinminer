package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.pattern.Shape
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
)

@Serializable
data class KeyPress(
    val pressed: Boolean,
    val shape: Shape,
    val maxDepth: Int = UNLIMITED_DEPTH,
) {
    companion object {
        /** Sentinel meaning "no depth cap" */
        const val UNLIMITED_DEPTH: Int = Int.MAX_VALUE
    }
}
