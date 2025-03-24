@file:Suppress("unused")

package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.VeinMinerEvent.veinmine
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.extensions.toNMS
import de.miraculixx.veinminer.config.extensions.toVeinminer
import de.miraculixx.veinminer.config.network.BlockHighlighting
import de.miraculixx.veinminer.config.network.ServerConfiguration
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.*

object FabricNetworking {
    val registeredPlayers: MutableMap<UUID, String> = mutableMapOf()
    val readyToVeinmine = mutableSetOf<UUID>()

    // Receive client handshake
    private val onJoin = PACKET_JOIN.receiveOnServer { packet, context ->
        val player = context.player
        val uuid = player.uuid

        val settings = ConfigManager.settings
        if (!settings.client.allow) return@receiveOnServer

        // Send information back
        Veinminer.LOGGER.info("${player.scoreboardName} joined with Veinminer version ${packet.veinminerClientVersion}")
        registeredPlayers[uuid] = packet.veinminerClientVersion
        sendConfiguration(context.player, settings)
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
        val player = context.player
        val level = player.level()
        val position = packet.blockPosition.toNMS()
        val state = level.getBlockState(position)

        val veinminerAction = VeinMinerEvent.allowedToVeinmine(level, player, position, state)
        if (veinminerAction == null) {
            PACKET_HIGHLIGHT.send(BlockHighlighting(false, "", emptyList()), context.player)
            return@receiveOnServer
        }

        veinminerAction.copy(settings = veinminerAction.settings.copy(delay = 0)).veinmine(false)
        val blocks = veinminerAction.processedBlocks.map { it.toVeinminer() }

        PACKET_HIGHLIGHT.send(BlockHighlighting(true, VeinMinerEvent.getPreferredToolIcon(state), blocks), context.player)
    }

    // Remove any player data when they disconnect
    fun onDisconnect(packet: ServerGamePacketListenerImpl, server: MinecraftServer) {
        val uuid = packet.player.uuid
        registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
    }

    fun sendConfiguration(player: ServerPlayer, settings: VeinminerSettings) {
        if (!registeredPlayers.containsKey(player.uuid)) return
        val conf = ServerConfiguration(settings.cooldown, settings.mustSneak, false, settings.client.translucentBlockHighlight)
        PACKET_CONFIGURATION.send(conf, player)
    }
}