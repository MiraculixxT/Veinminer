package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.network.BlockHighlighting
import de.miraculixx.veinminer.config.network.ServerConfiguration
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.*

object FabricNetworking {
    val registeredPlayers: MutableMap<UUID, String> = mutableMapOf()
    val readyToVeinmine = mutableSetOf<UUID>()

    // Receive client handshake
    private val onJoin = PACKET_JOIN.receiveOnServer { packet, context ->
        val uuid = context.player.uuid
        registeredPlayers[uuid] = packet.veinminerClientVersion
        Veinminer.LOGGER.info("$uuid joined with Veinminer version ${packet.veinminerClientVersion}")

        // Send information back
        val settings = ConfigManager.settings
        val conf = ServerConfiguration(settings.cooldown, settings.mustSneak, false)
        PACKET_CONFIGURATION.send(conf, context.player)
    }

    private val onPress = PACKET_KEY_PRESS.receiveOnServer { packet, context ->
        val uuid = context.player.uuid
        if (packet.pressed) readyToVeinmine.add(uuid)
        else readyToVeinmine.remove(uuid)
        Veinminer.LOGGER.info("$uuid pressed hotkey (${packet.pressed})")
    }

    private val onMine = PACKET_MINE.receiveOnServer { packet, context ->
        val uuid = context.player.uuid
        Veinminer.LOGGER.info("$uuid requested to veinmine block at ${packet.blockPosition}")

        // Send feedback
        PACKET_HIGHLIGHT.send(BlockHighlighting(true, "pickaxe", listOf(packet.blockPosition)), context.player)
    }

    // Remove any player data when they disconnect
    fun onDisconnect(packet: ServerGamePacketListenerImpl, server: MinecraftServer) {
        val uuid = packet.player.uuid
        registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
    }
}