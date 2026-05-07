package de.miraculixx.veinminer.networking

import java.util.concurrent.ConcurrentHashMap

/**
 * Single dispatch table for clientbound veinminer payloads on NeoForge.
 *
 * The base mod owns the NeoForge `playToClient` registration. Its handler calls
 * [dispatch] for both the wire path (PacketDistributor → in-memory/network) and
 * the singleplayer loopback shortcut. The client addon plugs its callback in
 * via [register] when [de.miraculixx.veinminer.network.ClientNetworkRouter.init]
 * calls the platform's `registerS2C`.
 */
object ClientPayloadDispatch {
    private val handlers: MutableMap<String, (ByteArray) -> Unit> = ConcurrentHashMap()

    fun register(channel: String, handler: (ByteArray) -> Unit) {
        handlers[channel] = handler
    }

    fun dispatch(channel: String, payload: ByteArray) {
        handlers[channel]?.invoke(payload)
    }
}
