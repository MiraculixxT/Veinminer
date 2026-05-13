package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.ClientPayloadDispatch
import de.miraculixx.veinminer.network.ClientPlatformNetwork
import de.miraculixx.veinminer.network.VeinminerPayload
import de.miraculixx.veinminer.network.payloadType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.concurrent.ConcurrentHashMap

object FabricClientPlatformNetwork : ClientPlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = ConcurrentHashMap()

    private fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    // C2S codec is registered by the base mod's FabricPlatformNetwork (real server-side handler).
    override fun registerC2S(channel: String) {
        typeFor(channel)
    }

    // S2C wire registration (codec + global receiver) is owned by the base mod, which routes
    // payloads to ClientPayloadDispatch. The addon just plugs its callback in.
    override fun registerS2C(channel: String, handler: (ByteArray) -> Unit) {
        typeFor(channel)
        ClientPayloadDispatch.register(channel, handler)
    }

    override fun sendC2S(channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        ClientPlayNetworking.send(VeinminerPayload(type, payload))
    }
}
