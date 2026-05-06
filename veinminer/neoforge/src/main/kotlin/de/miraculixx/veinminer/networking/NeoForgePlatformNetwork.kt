package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.network.LocalLoopback
import de.miraculixx.veinminer.network.NetworkManager
import de.miraculixx.veinminer.network.PlatformNetwork
import de.miraculixx.veinminer.network.VeinminerPayload
import de.miraculixx.veinminer.network.payloadType
import de.miraculixx.veinminer.network.rawBytesCodec
import de.miraculixx.veinminer.utils.mcServer
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NeoForgePlatformNetwork : PlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = ConcurrentHashMap()
    private val pendingC2S: MutableMap<String, (UUID, ByteArray) -> Unit> = ConcurrentHashMap()
    private val pendingS2C: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var registrar: PayloadRegistrar? = null

    fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    fun onRegisterPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        val reg = event.registrar(NetworkManager.PACKET_IDENTIFIER).optional()
        registrar = reg
        // Flush anything that registered before the event fired
        pendingC2S.forEach { (channel, handler) -> doRegisterC2S(reg, channel, handler) }
        pendingS2C.forEach { channel -> doRegisterS2C(reg, channel) }
    }

    override fun registerC2S(channel: String, handler: (UUID, ByteArray) -> Unit) {
        val reg = registrar
        if (reg == null) {
            pendingC2S[channel] = handler
        } else {
            doRegisterC2S(reg, channel, handler)
        }
    }

    override fun registerS2C(channel: String) {
        val reg = registrar
        if (reg == null) {
            pendingS2C.add(channel)
        } else {
            doRegisterS2C(reg, channel)
        }
    }

    override fun sendS2C(playerId: UUID, channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        if (LocalLoopback.isLoopbackPlayer(playerId)) {
            LocalLoopback.clientReceiver?.receive(channel, payload)
            return
        }
        val player = mcServer?.playerList?.getPlayer(playerId) ?: return
        PacketDistributor.sendToPlayer(player, VeinminerPayload(type, payload))
    }

    private fun doRegisterC2S(reg: PayloadRegistrar, channel: String, handler: (UUID, ByteArray) -> Unit) {
        val type = typeFor(channel)
        reg.playToServer(type, rawBytesCodec(type)) { payload: VeinminerPayload, ctx: IPayloadContext ->
            try {
                handler(ctx.player().uuid, payload.bytes)
            } catch (e: Exception) {
                Veinminer.LOGGER.warn("Failed to dispatch packet $channel: ${e.message}")
            }
        }
    }

    private fun doRegisterS2C(reg: PayloadRegistrar, channel: String) {
        val type = typeFor(channel)
        // Server only sends — register a no-op client handler that won't be invoked server-side.
        reg.playToClient(type, rawBytesCodec(type)) { _, _ -> }
    }
}
