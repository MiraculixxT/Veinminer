package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.command.ActiveHost
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NetworkRouter {
    val registeredPlayers: MutableMap<UUID, String> = ConcurrentHashMap()
    val readyToVeinmine: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var platform: PlatformNetwork? = null

    private val logger: Logger = ActiveHost.host.logger

    private val c2sHandlers: MutableMap<String, (UUID, ByteArray) -> Unit> = ConcurrentHashMap()

    fun init(platform: PlatformNetwork, callbacks: ServerCallbacks) {
        this.platform = platform

        registerC2S(platform, NetworkManager.PACKET_JOIN_ID) { uuid, bytes ->
            callbacks.onJoinAccepted(uuid, Codec.decode(bytes))
        }
        registerC2S(platform, NetworkManager.PACKET_KEY_PRESS_ID) { uuid, bytes ->
            val packet: KeyPress = Codec.decode(bytes)
            if (packet.pressed) readyToVeinmine.add(uuid) else readyToVeinmine.remove(uuid)
            callbacks.onKeyPress(uuid, packet)
        }
        registerC2S(platform, NetworkManager.PACKET_MINE_ID) { uuid, bytes ->
            callbacks.onMineRequest(uuid, Codec.decode(bytes))
        }
        platform.registerS2C(NetworkManager.PACKET_CONFIGURATION_ID)
        platform.registerS2C(NetworkManager.PACKET_HIGHLIGHT_ID)
    }

    private fun registerC2S(platform: PlatformNetwork, channel: String, handler: (UUID, ByteArray) -> Unit) {
        val safe: (UUID, ByteArray) -> Unit = { uuid, bytes ->
            try {
                handler(uuid, bytes)
            } catch (e: Exception) {
                logger.warn("Failed to handle C2S packet '$channel' from $uuid: ${e.message}")
            }
        }
        c2sHandlers[channel] = safe
        platform.registerC2S(channel, safe)
    }

    /** Loopback entry for the singleplayer client - bypasses packet stream */
    fun dispatchC2S(channel: String, playerId: UUID, payload: ByteArray) {
        c2sHandlers[channel]?.invoke(playerId, payload)
    }

    fun sendConfiguration(uuid: UUID, payload: ServerConfiguration) {
        val plat = platform
        if (plat == null) {
            logger.warn("sendConfiguration called before NetworkRouter.init()")
            return
        }
        if (!registeredPlayers.containsKey(uuid)) {
            logger.debug("sendConfiguration: $uuid not registered, dropping")
            return
        }
        plat.sendS2C(uuid, NetworkManager.PACKET_CONFIGURATION_ID, Codec.encode(payload))
    }

    fun sendHighlighting(uuid: UUID, payload: BlockHighlighting) {
        val plat = platform
        if (plat == null) {
            logger.warn("sendHighlighting called before NetworkRouter.init()")
            return
        }
        if (!registeredPlayers.containsKey(uuid)) {
            logger.debug("sendHighlighting: $uuid not registered, dropping")
            return
        }
        plat.sendS2C(uuid, NetworkManager.PACKET_HIGHLIGHT_ID, Codec.encode(payload))
    }

    fun onDisconnect(uuid: UUID) {
        registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
    }
}
