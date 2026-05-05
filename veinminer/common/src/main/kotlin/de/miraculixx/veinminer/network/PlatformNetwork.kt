package de.miraculixx.veinminer.network

import java.util.UUID

/**
 * Loader-side network adapter. Each platform (Fabric, NeoForge, Paper) implements
 * this to register channels and ship raw byte payloads. All encoding lives in [Codec].
 */
interface PlatformNetwork {
    fun registerC2S(channel: String, handler: (playerId: UUID, payload: ByteArray) -> Unit)
    fun registerS2C(channel: String)
    fun sendS2C(playerId: UUID, channel: String, payload: ByteArray)
}
