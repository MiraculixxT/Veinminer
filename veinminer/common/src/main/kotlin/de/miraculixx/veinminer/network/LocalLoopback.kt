package de.miraculixx.veinminer.network

import java.util.UUID

/**
 * Singleplayer shortcut. The client sets [clientReceiver] so the server side can
 * deliver S2C payloads without going through the wire. Platform adapters check
 * [isLoopbackPlayer] before falling back to the real channel.
 */
object LocalLoopback {
    interface ClientReceiver {
        fun receive(channel: String, payload: ByteArray)
    }

    @Volatile
    var clientReceiver: ClientReceiver? = null

    @Volatile
    var loopbackPlayer: UUID? = null

    fun isLoopbackPlayer(uuid: UUID): Boolean = loopbackPlayer != null && uuid == loopbackPlayer

    fun reset() {
        clientReceiver = null
        loopbackPlayer = null
    }
}
