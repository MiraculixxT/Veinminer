package de.miraculixx.veinminer.network

import java.util.UUID

object NetworkRouter {
    val registeredPlayers: MutableMap<UUID, String> = mutableMapOf()
    val readyToVeinmine: MutableSet<UUID> = mutableSetOf()

    private var platform: PlatformNetwork? = null
    private val c2sHandlers: MutableMap<String, (UUID, ByteArray) -> Unit> = mutableMapOf()

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
        c2sHandlers[channel] = handler
        platform.registerC2S(channel, handler)
    }

    /** Loopback entry for the singleplayer client — bypasses the wire. */
    fun dispatchC2S(channel: String, playerId: UUID, payload: ByteArray) {
        c2sHandlers[channel]?.invoke(playerId, payload)
    }

    fun sendConfiguration(uuid: UUID, payload: ServerConfiguration) {
        if (!registeredPlayers.containsKey(uuid)) return
        platform?.sendS2C(uuid, NetworkManager.PACKET_CONFIGURATION_ID, Codec.encode(payload))
    }

    fun sendHighlighting(uuid: UUID, payload: BlockHighlighting) {
        if (!registeredPlayers.containsKey(uuid)) return
        platform?.sendS2C(uuid, NetworkManager.PACKET_HIGHLIGHT_ID, Codec.encode(payload))
    }

    fun onDisconnect(uuid: UUID) {
        registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
    }
}
