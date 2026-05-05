package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.network.LocalLoopback
import de.miraculixx.veinminer.network.NetworkManager
import de.miraculixx.veinminer.network.PlatformNetwork
import de.miraculixx.veinminer.utils.ServerHolder
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.UUID

object FabricPlatformNetwork : PlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = mutableMapOf()

    private fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    override fun registerC2S(channel: String, handler: (UUID, ByteArray) -> Unit) {
        val type = typeFor(channel)
        PayloadTypeRegistry.playC2S().register(type, rawBytesCodec(type))
        ServerPlayNetworking.registerGlobalReceiver(type) { payload, ctx ->
            handler(ctx.player().uuid, payload.bytes)
        }
    }

    override fun registerS2C(channel: String) {
        val type = typeFor(channel)
        PayloadTypeRegistry.playS2C().register(type, rawBytesCodec(type))
    }

    override fun sendS2C(playerId: UUID, channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        if (LocalLoopback.isLoopbackPlayer(playerId)) {
            LocalLoopback.clientReceiver?.receive(channel, payload)
            return
        }
        val player = ServerHolder.server?.playerList?.getPlayer(playerId) ?: return
        ServerPlayNetworking.send(player, VeinminerPayload(type, payload))
    }
}
