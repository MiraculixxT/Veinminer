package de.miraculixx.veinminer.network

import java.util.UUID

/**
 * Singleplayer shortcut marker. The client sets [loopbackPlayer] when issuing
 * a C2S packet via the bypass path so the server-side `sendS2C` knows to
 * dispatch the response through [ClientPayloadDispatch] instead of the wire.
 */
object LocalLoopback {
    @Volatile
    var loopbackPlayer: UUID? = null

    fun isLoopbackPlayer(uuid: UUID): Boolean = loopbackPlayer != null && uuid == loopbackPlayer

    fun reset() {
        loopbackPlayer = null
    }
}
