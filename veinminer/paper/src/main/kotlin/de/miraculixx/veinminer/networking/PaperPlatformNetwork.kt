package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.network.PlatformNetwork
import de.miraculixx.veinminer.utils.IDENTIFIER
import io.netty.buffer.Unpooled
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.DiscardedPayload
import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
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
        // Skip Bukkits channel registration check
        val packet = ClientboundCustomPayloadPacket(
            DiscardedPayload(ResourceLocation.parse(channelOf(channel)), Unpooled.wrappedBuffer(payload))
        )
        (player as CraftPlayer).handle.connection.send(packet)
    }
}
