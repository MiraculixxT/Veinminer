package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.ClientPlatformNetwork
import de.miraculixx.veinminer.network.VeinminerPayload
import de.miraculixx.veinminer.network.payloadType
import de.miraculixx.veinminer.network.rawBytesCodec
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.concurrent.ConcurrentHashMap

object FabricClientPlatformNetwork : ClientPlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = ConcurrentHashMap()
    private val registeredC2S: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val registeredS2C: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    override fun registerC2S(channel: String) {
        val type = typeFor(channel)
        if (registeredC2S.add(channel)) {
            // Integrated server may already have registered this type via FabricPlatformNetwork.
            try {
                PayloadTypeRegistry.serverboundPlay().register(type, rawBytesCodec(type))
            } catch (_: IllegalArgumentException) {
                // Already registered server-side in this JVM (singleplayer) - fine.
            }
        }
    }

    override fun registerS2C(channel: String, handler: (ByteArray) -> Unit) {
        val type = typeFor(channel)
        if (registeredS2C.add(channel)) {
            try {
                PayloadTypeRegistry.clientboundPlay().register(type, rawBytesCodec(type))
            } catch (_: IllegalArgumentException) {
                // Already registered server-side in this JVM (singleplayer) - fine.
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(type) { payload, _ ->
            handler(payload.bytes)
        }
    }

    override fun sendC2S(channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        ClientPlayNetworking.send(VeinminerPayload(type, payload))
    }
}
