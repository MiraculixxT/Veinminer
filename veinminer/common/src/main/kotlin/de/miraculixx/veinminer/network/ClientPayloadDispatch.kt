package de.miraculixx.veinminer.network

import java.util.concurrent.ConcurrentHashMap

/**
 * Single dispatch table for clientbound veinminer payloads.
 *
 * Each loader's base-mod platform registers ONE wire-level clientbound handler
 * that routes payloads here. The client addon plugs its callback in via
 * [register] when [ClientNetworkRouter.init] calls `registerS2C`. The
 * singleplayer loopback shortcut in `sendS2C` also dispatches here, so wire
 * and loopback paths share the same per-channel handler.
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
