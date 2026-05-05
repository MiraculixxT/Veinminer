package de.miraculixx.veinminer.network

/**
 * Loader-side client network adapter. Each client platform (Fabric, NeoForge)
 * implements this to register channels and ship raw byte payloads in either
 * direction. All encoding lives in [Codec].
 */
interface ClientPlatformNetwork {
    /** Register a C2S channel so the client may send on it. */
    fun registerC2S(channel: String)

    /** Register an S2C handler. The handler receives the raw payload bytes. */
    fun registerS2C(channel: String, handler: (ByteArray) -> Unit)

    /** Send raw bytes to the connected server on the given channel. */
    fun sendC2S(channel: String, payload: ByteArray)
}
