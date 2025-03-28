@file:Suppress("unused")

package de.miraculixx.veinminer.networking

import de.miraculixx.kpaper.event.listen
import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.VeinMinerEvent.veinmine
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.network.*
import de.miraculixx.veinminer.config.utils.debug
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import java.io.ByteArrayOutputStream
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
object PaperNetworking {
    val registeredPlayers: MutableMap<UUID, String> = mutableMapOf()
    val readyToVeinmine = mutableSetOf<UUID>()

    private val onJoinPacket = c2sPacket<JoinInformation>(NetworkManager.PACKET_JOIN_ID) { player, packet ->
        val uuid = player.uniqueId

        val settings = ConfigManager.settings
        if (!settings.client.allow) return@c2sPacket

        // Send information back
        Veinminer.INSTANCE.logger.info("${player.name} joined with Veinminer version ${packet.veinminerClientVersion}")
        registeredPlayers[uuid] = packet.veinminerClientVersion
        sendConfiguration(player, settings)
    }

    private val onPress = c2sPacket<KeyPress>(NetworkManager.PACKET_KEY_PRESS_ID) { player, packet ->
        val uuid = player.uniqueId
        if (packet.pressed) readyToVeinmine.add(uuid)
        else readyToVeinmine.remove(uuid)
        if (debug) Veinminer.INSTANCE.logger.info("$uuid pressed hotkey (${packet.pressed})")
    }

    private val onMine = c2sPacket<RequestBlockVein>(NetworkManager.PACKET_MINE_ID) { player, packet ->
        val uuid = player.uniqueId
        if (debug) Veinminer.INSTANCE.logger.info("$uuid requested to veinmine block at ${packet.blockPosition}")

        // Send feedback
        val position = packet.blockPosition
        val block = player.world.getBlockAt(position.x, position.y, position.z)
        val veinminerAction = VeinMinerEvent.allowedToVeinmine(player, block)
        if (veinminerAction == null) {
            PACKET_HIGHLIGHT.send(BlockHighlighting(false, "", emptyList()), player)
            return@c2sPacket
        }

        veinminerAction.copy(settings = veinminerAction.settings.copy(delay = 0)).veinmine(false)
        val blocks = veinminerAction.processedBlocks.map { BlockPosition(it.x, it.y, it.z) }

        PACKET_HIGHLIGHT.send(BlockHighlighting(true, VeinMinerEvent.getPreferredToolIcon(block.type), blocks), player)
    }

    // Remove any player data when they disconnect
    private val onDisconnect = listen<PlayerQuitEvent> {
        val uuid = it.player.uniqueId
        registeredPlayers.remove(uuid)
        readyToVeinmine.remove(uuid)
    }

    fun sendConfiguration(player: Player, settings: VeinminerSettings) {
        if (!registeredPlayers.containsKey(player.uniqueId)) return
        val conf = ServerConfiguration(settings.cooldown, settings.mustSneak, false, settings.client.translucentBlockHighlight)
        PACKET_CONFIGURATION.send(conf, player)
    }

    /**
     * Utility function to send a packet of type T to a player
     */
    private inline fun <reified T> String.send(data: T, player: Player) {
        try {

            // Encode the data to CBOR bytes
            val payload = Cbor.encodeToByteArray<T>(data)
            val baos = ByteArrayOutputStream()

            // Write the payload size as a VarInt
            writeVarInt(baos, payload.size)
            // Write the payload itself
            baos.write(payload)

            // Send to player
            if (debug) Veinminer.INSTANCE.logger.info("Sending packet $this (${payload.size}) to ${player.name}")
            player.sendPluginMessage(Veinminer.INSTANCE, this, baos.toByteArray())

        } catch (e: Exception) {
            Veinminer.INSTANCE.logger.warning("Failed to encode packet $this: ${e.message}")
            e.printStackTrace()
        }
    }

    // VarInt encoding implementation
    private fun writeVarInt(outputStream: ByteArrayOutputStream, value: Int) {
        var val1 = value
        do {
            var temp = (val1 and 0x7F)
            val1 = val1 ushr 7
            if (val1 != 0) {
                temp = temp or 0x80
            }
            outputStream.write(temp)
        } while (val1 != 0)
    }
}