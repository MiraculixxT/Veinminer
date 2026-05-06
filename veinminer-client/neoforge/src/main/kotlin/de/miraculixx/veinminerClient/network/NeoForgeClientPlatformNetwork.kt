package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.ClientPlatformNetwork
import de.miraculixx.veinminer.network.NetworkManager as CommonNetworkManager
import de.miraculixx.veinminer.network.VeinminerPayload
import de.miraculixx.veinminer.network.payloadType
import de.miraculixx.veinminer.network.rawBytesCodec
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import java.util.concurrent.ConcurrentHashMap

object NeoForgeClientPlatformNetwork : ClientPlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = ConcurrentHashMap()
    private val pendingC2S: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val pendingS2C: MutableMap<String, (ByteArray) -> Unit> = ConcurrentHashMap()

    @Volatile
    private var registrar: PayloadRegistrar? = null

    private fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    fun onRegisterPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        val reg = event.registrar(CommonNetworkManager.PACKET_IDENTIFIER).optional()
        registrar = reg
        pendingC2S.forEach { channel -> doRegisterC2S(reg, channel) }
        pendingS2C.forEach { (channel, handler) -> doRegisterS2C(reg, channel, handler) }
    }

    override fun registerC2S(channel: String) {
        val reg = registrar
        if (reg == null) {
            pendingC2S.add(channel)
        } else {
            doRegisterC2S(reg, channel)
        }
    }

    override fun registerS2C(channel: String, handler: (ByteArray) -> Unit) {
        val reg = registrar
        if (reg == null) {
            pendingS2C[channel] = handler
        } else {
            doRegisterS2C(reg, channel, handler)
        }
    }

    override fun sendC2S(channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        val connection = Minecraft.getInstance().connection ?: return
        connection.send(ServerboundCustomPayloadPacket(VeinminerPayload(type, payload)))
    }

    private fun doRegisterC2S(reg: PayloadRegistrar, channel: String) {
        val type = typeFor(channel)
        // Server-bound: register a no-op server handler since this object is the client side.
        reg.playToServer(type, rawBytesCodec(type)) { _: VeinminerPayload, _: IPayloadContext -> }
    }

    private fun doRegisterS2C(reg: PayloadRegistrar, channel: String, handler: (ByteArray) -> Unit) {
        val type = typeFor(channel)
        reg.playToClient(type, rawBytesCodec(type)) { payload: VeinminerPayload, _: IPayloadContext ->
            handler(payload.bytes)
        }
    }
}
