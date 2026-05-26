package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.network.PlatformNetwork
import de.miraculixx.veinminer.utils.IDENTIFIER
import org.bukkit.Bukkit
import java.util.UUID

object PaperPlatformNetwork : PlatformNetwork {
    private fun channelOf(packetId: String): String = "$IDENTIFIER:$packetId"

    override fun registerC2S(channel: String, handler: (UUID, ByteArray) -> Unit) {
        val plugin = Veinminer.INSTANCE
        plugin.server.messenger.registerIncomingPluginChannel(plugin, channelOf(channel)) { _, player, message ->
            try {
                handler(player.uniqueId, message)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to dispatch packet ${channelOf(channel)}: ${e.message}")
            }
        }
    }

    override fun registerS2C(channel: String) {
        val plugin = Veinminer.INSTANCE
        plugin.server.messenger.registerOutgoingPluginChannel(plugin, channelOf(channel))
    }

    override fun sendS2C(playerId: UUID, channel: String, payload: ByteArray) {
        val player = Bukkit.getPlayer(playerId) ?: return
        player.sendPluginMessage(Veinminer.INSTANCE, channelOf(channel), payload)
    }
}
