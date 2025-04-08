@file:Suppress("unused")

package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.VeinMinerEvent.veinmine
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.extensions.toNMS
import de.miraculixx.veinminer.config.extensions.toVeinminer
import de.miraculixx.veinminer.config.network.BlockHighlighting
import de.miraculixx.veinminer.config.network.JoinInformation
import de.miraculixx.veinminer.config.network.KeyPress
import de.miraculixx.veinminer.config.network.RequestBlockVein
import de.miraculixx.veinminer.config.network.ServerConfiguration
import de.miraculixx.veinminer.config.utils.debug
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.silkmc.silk.core.Silk
import java.util.*

object FabricNetworking {
    val registeredPlayers: MutableMap<UUID, String> = mutableMapOf()
    val readyToVeinmine = mutableSetOf<UUID>()

    //
    // Receive packets from clients
    //
    private val onJoin = PACKET_JOIN.receiveOnServer { packet, context -> onJoin(context.player, packet) }
    private val onPress = PACKET_KEY_PRESS.receiveOnServer { packet, context -> onPress(context.player.uuid, packet) }
    private val onMine = PACKET_MINE.receiveOnServer { packet, context -> onMine(context.player, packet) }

    // Receive client handshake
    fun onJoin(player: ServerPlayer?, packet: JoinInformation, uuid: UUID? = null) {
        val uuid = player?.uuid ?: uuid ?: return invalidUserInformation("join")
        val player = player ?: Silk.server?.playerList?.getPlayer(uuid) ?: return invalidUserInformation("join player")

        val settings = ConfigManager.settings
        if (!settings.client.allow) return

        // Send information back
        Veinminer.LOGGER.info("${player.scoreboardName} joined with Veinminer version ${packet.veinminerClientVersion}")
        registeredPlayers[uuid] = packet.veinminerClientVersion
        sendConfiguration(player, settings)
    }

    // Receive key press (true & false)
    fun onPress(uuid: UUID?, packet: KeyPress) {
        if (uuid == null) return invalidUserInformation("key press")
        if (debug) Veinminer.LOGGER.info("$uuid pressed hotkey (${packet.pressed})")
        if (packet.pressed) readyToVeinmine.add(uuid)
        else readyToVeinmine.remove(uuid)
    }

    // Receive veinmine highlight request
    fun onMine(player: ServerPlayer?, packet: RequestBlockVein, uuid: UUID? = null) {
        val uuid = player?.uuid ?: uuid ?: return invalidUserInformation("mine")
        val player = player ?: Silk.server?.playerList?.getPlayer(uuid) ?: return invalidUserInformation("mine player")

        if (debug) Veinminer.LOGGER.info("$uuid requested to veinmine block at ${packet.blockPosition}")

        // Send feedback
        val level = player.level()
        val position = packet.blockPosition.toNMS()
        val state = level.getBlockState(position)

        val veinminerAction = VeinMinerEvent.allowedToVeinmine(level, player, position, state)
        if (veinminerAction == null) {
            sendHighlighting(player, false, "", emptyList())
            return
        }

        veinminerAction.copy(settings = veinminerAction.settings.copy(delay = 0)).veinmine(false)
        val blocks = veinminerAction.processedBlocks.map { it.toVeinminer() }

        sendHighlighting(player, true, VeinMinerEvent.getPreferredToolIcon(state), blocks)
    }

    // Remove any player data when they disconnect
    fun onDisconnect(packet: ServerGamePacketListenerImpl, server: MinecraftServer) {
        val uuid = packet.player.uuid
        val removed = registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
        if (debug && removed != null) Veinminer.LOGGER.info("Removed ${packet.player.scoreboardName} from registered players")
    }

    //
    // Send packets to clients
    //
    fun sendConfiguration(player: ServerPlayer, settings: VeinminerSettings) {
        if (!registeredPlayers.containsKey(player.uuid)) return
        val conf = ServerConfiguration(settings.cooldown, settings.mustSneak, false, settings.client.translucentBlockHighlight)
        PACKET_CONFIGURATION.send(conf, player)
    }

    fun sendHighlighting(player: ServerPlayer, allowed: Boolean, icon: String, blocks: List<BlockPosition>) {
        if (!registeredPlayers.containsKey(player.uuid)) return userNotConnected(player.uuid)
        PACKET_HIGHLIGHT.send(BlockHighlighting(allowed, icon, blocks), player)
    }

    //
    // Error logging
    //
    private fun invalidUserInformation(type: String) {
        Veinminer.LOGGER.warn("Not enough information to handle '$type' packet!")
    }

    private fun userNotConnected(uuid: UUID) {
        Veinminer.LOGGER.warn("Can not send packet to client $uuid (not connected)")
    }
}