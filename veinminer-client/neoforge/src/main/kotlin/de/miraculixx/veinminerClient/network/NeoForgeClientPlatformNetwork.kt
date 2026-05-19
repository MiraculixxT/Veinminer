package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.ClientNetworkRouter
import de.miraculixx.veinminer.network.ClientPlatformNetwork
import de.miraculixx.veinminer.network.VeinminerPayload
import de.miraculixx.veinminer.network.payloadType
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.concurrent.ConcurrentHashMap

object NeoForgeClientPlatformNetwork : ClientPlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = ConcurrentHashMap()

    private fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    // C2S codec is registered by the base mod's NeoForgePlatformNetwork (real server-side handler).
    override fun registerC2S(channel: String) {
        typeFor(channel)
    }

    // S2C wire registration is owned by the base mod. The addon just plugs its callback in.
    override fun registerS2C(channel: String, handler: (ByteArray) -> Unit) {
        typeFor(channel)
        ClientNetworkRouter.registerClientboundHandler(channel, handler)
    }

    override fun sendC2S(channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        val connection = Minecraft.getInstance().connection ?: return
        connection.send(ServerboundCustomPayloadPacket(VeinminerPayload(type, payload)))
    }
}
