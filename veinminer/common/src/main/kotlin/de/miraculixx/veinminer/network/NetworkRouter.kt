package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.pattern.PatternConfig
import de.miraculixx.veinminer.pattern.PatternType
import de.miraculixx.veinminer.pattern.ShapeStrategy
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.utils.mcServer
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NetworkRouter {
    val registeredPlayers: MutableMap<UUID, String> = ConcurrentHashMap()
    private val readyToVeinmine: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val activeStrategies: MutableMap<UUID, ShapeStrategy> = ConcurrentHashMap()
    private val clientPatterns: MutableMap<UUID, Map<String, PatternConfig>> = ConcurrentHashMap()
    val lastSurface: MutableMap<UUID, Surface> = ConcurrentHashMap()
    val activeDepth: MutableMap<UUID, Int> = ConcurrentHashMap()

    fun activeStrategy(uuid: UUID): ShapeStrategy? = activeStrategies[uuid]
    fun maxDepth(uuid: UUID): Int = activeDepth[uuid] ?: Int.MAX_VALUE
    fun isReady(uuid: UUID): Boolean = readyToVeinmine.contains(uuid)

    @Volatile
    private var platform: PlatformNetwork? = null

    private val logger: Logger = ActiveHost.host.logger

    private val c2sHandlers: MutableMap<String, (UUID, ByteArray) -> Unit> = ConcurrentHashMap()

    fun init(platform: PlatformNetwork, callbacks: ServerCallbacks) {
        this.platform = platform

        registerC2S(platform, NetworkManager.PACKET_JOIN_ID) { uuid, bytes ->
            callbacks.onJoinAccepted(uuid, PacketCodecs.JOIN.decode(bytes))
        }
        registerC2S(platform, NetworkManager.PACKET_PATTERNS_ID) { uuid, bytes ->
            if (!registeredPlayers.containsKey(uuid)) return@registerC2S
            val packet = PacketCodecs.PATTERNS.decode(bytes)
            clientPatterns[uuid] = validatePatterns(packet.patterns)
        }
        registerC2S(platform, NetworkManager.PACKET_KEY_PRESS_ID) { uuid, bytes ->
            val packet = PacketCodecs.KEY.decode(bytes)
            if (packet.pressed) {
                val strategy = resolveStrategy(uuid, packet)
                if (strategy == null) {
                    clearReadyState(uuid)
                    return@registerC2S
                }
                readyToVeinmine.add(uuid)
                activeStrategies[uuid] = strategy
                activeDepth[uuid] = packet.maxDepth.coerceIn(2..Int.MAX_VALUE)
            } else {
                clearReadyState(uuid)
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
        clearReadyState(uuid)
        clientPatterns.remove(uuid)
        lastSurface.remove(uuid)
    }

    private fun clearReadyState(uuid: UUID) {
        readyToVeinmine.remove(uuid)
        activeStrategies.remove(uuid)
        activeDepth.remove(uuid)
    }

    private fun resolveStrategy(uuid: UUID, packet: KeyPress): ShapeStrategy? {
        val patternId = packet.patternId ?: return null
        return clientPatterns[uuid]?.get(patternId)?.strategy()
    }

    private fun validatePatterns(patterns: List<PatternConfig>): Map<String, PatternConfig> {
        if (patterns.isEmpty()) return emptyMap()
        val valid = LinkedHashMap<String, PatternConfig>()
        patterns.take(MAX_CLIENT_PATTERNS).forEach { raw ->
            val id = raw.id.takeIf(::validPatternId) ?: return@forEach
            if (valid.containsKey(id)) return@forEach
            valid[id] = PatternConfig(
                id = id,
                enabled = raw.enabled,
                type = raw.type,
                color = raw.color and 0xFFFFFF,
                width = sanitizeDimension(raw.type, raw.width),
                height = sanitizeDimension(raw.type, raw.height),
                stairsUp = raw.stairsUp,
            )
        }
        return valid
    }

    private fun sanitizeDimension(type: PatternType, value: Int): Int = when (type) {
        PatternType.NORMAL,
        PatternType.FLAT -> 1
        PatternType.TUNNEL,
        PatternType.STAIRS -> value.coerceIn(MIN_PATTERN_SIZE, MAX_PATTERN_SIZE)
    }

    private fun validPatternId(id: String): Boolean =
        id.isNotBlank() && id.length <= MAX_PATTERN_ID_LENGTH && id.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' || it == ':' }

    private const val MAX_CLIENT_PATTERNS = 64
    private const val MAX_PATTERN_ID_LENGTH = 64
    private const val MIN_PATTERN_SIZE = 1
    private const val MAX_PATTERN_SIZE = 10
}
