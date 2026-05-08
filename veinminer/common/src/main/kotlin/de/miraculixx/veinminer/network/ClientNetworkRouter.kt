package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.command.ActiveHost
import org.slf4j.Logger
import java.util.UUID

/**
 * Mirror of [NetworkRouter] for the client side. Centralizes encode/decode +
 * loopback handling so loader-leaf modules only own the tiny
 * [ClientPlatformNetwork] adapter and the [ClientCallbacks] state.
 *
 * TODO: Moving into client common module. But needing to depend on base common module for this?
 */
object ClientNetworkRouter {
    @Volatile
    private var platform: ClientPlatformNetwork? = null

    private val logger: Logger = ActiveHost.host.logger

    /**
     * Predicate for "is this a singleplayer integrated server with the base mod
     * available". When true, send paths bypass the wire and dispatch through
     * [NetworkRouter.dispatchC2S]; receive paths come back via [LocalLoopback].
     */
    @Volatile
    private var loopbackPredicate: () -> Boolean = { false }

    @Volatile
    private var localPlayerId: () -> UUID? = { null }

    fun init(
        platform: ClientPlatformNetwork,
        callbacks: ClientCallbacks,
        loopbackPredicate: () -> Boolean,
        localPlayerId: () -> UUID?
    ) {
        this.platform = platform
        this.loopbackPredicate = loopbackPredicate
        this.localPlayerId = localPlayerId

        platform.registerC2S(NetworkManager.PACKET_JOIN_ID)
        platform.registerC2S(NetworkManager.PACKET_KEY_PRESS_ID)
        platform.registerC2S(NetworkManager.PACKET_MINE_ID)

        platform.registerS2C(NetworkManager.PACKET_CONFIGURATION_ID) { bytes ->
            try {
                callbacks.onConfiguration(Codec.decode(bytes))
            } catch (e: Exception) {
                logger.warn("Failed to decode S2C 'configuration': ${e.message}")
            }
        }
        platform.registerS2C(NetworkManager.PACKET_HIGHLIGHT_ID) { bytes ->
            try {
                callbacks.onHighlight(Codec.decode(bytes))
            } catch (e: Exception) {
                logger.warn("Failed to decode S2C 'highlight': ${e.message}")
            }
        }

    }

    fun sendJoin(version: String) {
        send(NetworkManager.PACKET_JOIN_ID, JoinInformation(version), markLoopback = true)
    }

    fun sendKeyPress(pressed: Boolean) {
        send(NetworkManager.PACKET_KEY_PRESS_ID, KeyPress(pressed))
    }

    fun sendBlockRequest(packet: RequestBlockVein) {
        send(NetworkManager.PACKET_MINE_ID, packet)
    }

    private inline fun <reified T> send(channel: String, packet: T, markLoopback: Boolean = false) {
        val bytes = Codec.encode(packet)
        val plat = platform
        if (plat == null) {
            logger.warn("Send '$channel' called before ClientNetworkRouter.init()")
            return
        }
        if (loopbackPredicate()) {
            val uuid = localPlayerId() ?: return
            if (markLoopback) {
                LocalLoopback.loopbackPlayer = uuid
            }
            NetworkRouter.dispatchC2S(channel, uuid, bytes)
        } else {
            plat.sendC2S(channel, bytes)
        }
    }

    fun onDisconnect() {
        LocalLoopback.reset()
    }
}
