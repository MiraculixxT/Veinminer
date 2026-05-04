package de.miraculixx.veinminer.networking

import de.miraculixx.kpaper.extensions.onlinePlayers
import de.miraculixx.kpaper.extensions.server
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.network.NetworkManager
import de.miraculixx.veinminer.config.utils.IDENTIFIER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.bukkit.entity.Player

val PACKET_CONFIGURATION = s2cPacket(NetworkManager.PACKET_CONFIGURATION_ID)
val PACKET_HIGHLIGHT = s2cPacket(NetworkManager.PACKET_HIGHLIGHT_ID)

fun s2cPacket(packetID: String): String {
    val identifier = "$IDENTIFIER:$packetID"
    server.messenger.registerOutgoingPluginChannel(Veinminer.INSTANCE, identifier)
    onlinePlayers.forEach {
        it.listeningPluginChannels
    }

    return identifier
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> c2sPacket(packetID: String, crossinline event: (player: Player, packet: T) -> Unit) {
    val identifier = "$IDENTIFIER:$packetID"
    server.messenger.registerIncomingPluginChannel(Veinminer.INSTANCE, identifier) { channel, player, message ->
        // Skip first 1 byte header
        val payload = message.copyOfRange(1, message.size)

        try {
            // Decode the CBOR data into the appropriate class
            val data = Cbor.decodeFromByteArray<T>(payload)
            event.invoke(player, data)

        } catch (e: Exception) {
            Veinminer.INSTANCE.logger.warning("Failed to decode packet $identifier: ${e.message}")
            e.printStackTrace()
        }
    }
}
