package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.network.ClientPayloadDispatch
import de.miraculixx.veinminer.network.LocalLoopback
import de.miraculixx.veinminer.network.PlatformNetwork
import de.miraculixx.veinminer.network.VeinminerPayload
import de.miraculixx.veinminer.network.payloadType
import de.miraculixx.veinminer.network.rawBytesCodec
import de.miraculixx.veinminer.utils.mcServer
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FabricPlatformNetwork : PlatformNetwork {
    private val types: MutableMap<String, CustomPacketPayload.Type<VeinminerPayload>> = ConcurrentHashMap()
    private val registeredC2S: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val registeredS2C: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun typeFor(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
        types.getOrPut(channel) { payloadType(channel) }

    override fun registerC2S(channel: String, handler: (UUID, ByteArray) -> Unit) {
        val type = typeFor(channel)
        if (registeredC2S.add(channel)) {
            PayloadTypeRegistry.serverboundPlay().register(type, rawBytesCodec(type))
        }
        ServerPlayNetworking.registerGlobalReceiver(type) { payload, ctx ->
            try {
                handler(ctx.player().uuid, payload.bytes)
            } catch (e: Exception) {
                Veinminer.LOGGER.warn("Failed to dispatch packet $channel: ${e.message}")
            }
        }
    }

    override fun registerS2C(channel: String) {
        val type = typeFor(channel)
        if (registeredS2C.add(channel)) {
            PayloadTypeRegistry.clientboundPlay().register(type, rawBytesCodec(type))
            // The base mod owns the single canonical clientbound receiver in any client-side
            // environment (singleplayer integrated server, or a Fabric client install carrying
            // the base mod). The receiver routes payloads to ClientPayloadDispatch where the
            // veinminer-client addon plugs its callbacks. This avoids cross-mod registration
            // races and makes Fabric/NeoForge wire dispatch symmetric.
            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                ClientPlayNetworking.registerGlobalReceiver(type) { payload, _ ->
                    ClientPayloadDispatch.dispatch(channel, payload.bytes)
                }
            }
        }
    }

    override fun sendS2C(playerId: UUID, channel: String, payload: ByteArray) {
        val type = typeFor(channel)
        if (LocalLoopback.isLoopbackPlayer(playerId)) {
            ClientPayloadDispatch.dispatch(channel, payload)
            return
        }
        val player = mcServer?.playerList?.getPlayer(playerId) ?: return
        ServerPlayNetworking.send(player, VeinminerPayload(type, payload))
    }
}
