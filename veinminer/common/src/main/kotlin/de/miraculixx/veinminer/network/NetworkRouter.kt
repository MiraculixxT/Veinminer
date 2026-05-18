package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.utils.mcServer
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NetworkRouter {
    val registeredPlayers: MutableMap<UUID, String> = ConcurrentHashMap()
    val readyToVeinmine: MutableMap<UUID, Shape> = ConcurrentHashMap()
    val lastSurface: MutableMap<UUID, Surface> = ConcurrentHashMap()
    val activeDepth: MutableMap<UUID, Int> = ConcurrentHashMap()

    fun activeShape(uuid: UUID): Shape? = readyToVeinmine[uuid]
    fun maxDepth(uuid: UUID): Int = activeDepth[uuid] ?: Int.MAX_VALUE
    fun isReady(uuid: UUID): Boolean = readyToVeinmine.containsKey(uuid)

    @Volatile
    private var platform: PlatformNetwork? = null

    private val logger: Logger = ActiveHost.host.logger

    private val c2sHandlers: MutableMap<String, (UUID, ByteArray) -> Unit> = ConcurrentHashMap()

    fun init(platform: PlatformNetwork, callbacks: ServerCallbacks) {
        this.platform = platform

        registerC2S(platform, NetworkManager.PACKET_JOIN_ID) { uuid, bytes ->
            callbacks.onJoinAccepted(uuid, PacketCodecs.JOIN.decode(bytes))
        }
        registerC2S(platform, NetworkManager.PACKET_KEY_PRESS_ID) { uuid, bytes ->
            val packet = PacketCodecs.KEY.decode(bytes)
            if (packet.pressed) {
                readyToVeinmine[uuid] = packet.shape
                activeDepth[uuid] = packet.maxDepth
            } else {
                readyToVeinmine.remove(uuid)
                activeDepth.remove(uuid)
            }
            lastSurface[uuid] = packet.surface
            callbacks.onKeyPress(uuid, packet)
        }
        platform.registerS2C(NetworkManager.PACKET_CONFIGURATION_ID)
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

    fun dispatchC2S(channel: String, playerId: UUID, payload: ByteArray) {
        val handler = c2sHandlers[channel] ?: return
        val server = mcServer
        if (server != null && !server.isSameThread) {
            server.execute { handler(playerId, payload) }
        } else {
            handler(playerId, payload)
        }
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
        plat.sendS2C(uuid, NetworkManager.PACKET_CONFIGURATION_ID, PacketCodecs.CONFIGURATION.encode(payload))
    }

    fun onDisconnect(uuid: UUID) {
        registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
        lastSurface.remove(uuid)
        activeDepth.remove(uuid)
    }
}
