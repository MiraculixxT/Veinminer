@file:Suppress("unused")

package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.BlockHighlighting
import de.miraculixx.veinminer.network.Codec
import de.miraculixx.veinminer.network.JoinInformation
import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.network.LocalLoopback
import de.miraculixx.veinminer.network.NetworkManager as CoreNetworkManager
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.RequestBlockVein
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.networking.VeinminerPayload
import de.miraculixx.veinminer.networking.payloadType
import de.miraculixx.veinminer.networking.rawBytesCodec
import de.miraculixx.veinminer.pattern.Pattern
import de.miraculixx.veinminer.utils.debug
import de.miraculixx.veinminerClient.VeinminerClient
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDRenderer
import de.miraculixx.veinminerClient.utils.toVeinminer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object NetworkManager {
    // Client info
    var isVeinminerActive = false
        private set
    var selectedPattern = Pattern.DEFAULT

    // Server info
    var mustSneak = false
        private set
    var cooldown = 0
        private set
    var translucentBlockHighlight = true
        private set

    private val joinType = payloadType(CoreNetworkManager.PACKET_JOIN_ID)
    private val mineType = payloadType(CoreNetworkManager.PACKET_MINE_ID)
    private val keyType = payloadType(CoreNetworkManager.PACKET_KEY_PRESS_ID)
    private val configurationType: CustomPacketPayload.Type<VeinminerPayload> = payloadType(CoreNetworkManager.PACKET_CONFIGURATION_ID)
    private val highlightType: CustomPacketPayload.Type<VeinminerPayload> = payloadType(CoreNetworkManager.PACKET_HIGHLIGHT_ID)

    private var registered = false

    fun registerClientPayloads() {
        if (registered) return
        registered = true
        // C2S types must be registered for the client to send them. The server side
        // also registers these via FabricPlatformNetwork in the integrated server,
        // but we register defensively in case the dedicated-server path runs first.
        PayloadTypeRegistry.playC2S().register(joinType, rawBytesCodec(joinType))
        PayloadTypeRegistry.playC2S().register(mineType, rawBytesCodec(mineType))
        PayloadTypeRegistry.playC2S().register(keyType, rawBytesCodec(keyType))
        PayloadTypeRegistry.playS2C().register(configurationType, rawBytesCodec(configurationType))
        PayloadTypeRegistry.playS2C().register(highlightType, rawBytesCodec(highlightType))

        ClientPlayNetworking.registerGlobalReceiver(configurationType) { payload, ctx ->
            onConfiguration(Codec.decode(payload.bytes), ctx.client())
        }
        ClientPlayNetworking.registerGlobalReceiver(highlightType) { payload, _ ->
            onHighlight(Codec.decode(payload.bytes))
        }
    }

    // Loopback receiver for singleplayer: server side delivers S2C payloads in-JVM
    private val loopbackReceiver = object : LocalLoopback.ClientReceiver {
        override fun receive(channel: String, payload: ByteArray) {
            when (channel) {
                CoreNetworkManager.PACKET_CONFIGURATION_ID ->
                    onConfiguration(Codec.decode(payload), Minecraft.getInstance())
                CoreNetworkManager.PACKET_HIGHLIGHT_ID ->
                    onHighlight(Codec.decode(payload))
            }
        }
    }

    fun onConfiguration(packet: ServerConfiguration, client: Minecraft) {
        VeinminerClient.LOGGER.info("Server configuration: $packet")
        if (packet.outdated) {
            client.toastManager.addToast(
                SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("Veinminer Outdated"), Component.literal("Please update Veinminer"))
            )
        }

        isVeinminerActive = true
        mustSneak = packet.mustSneak
        cooldown = packet.cooldown
        translucentBlockHighlight = packet.translucentBlockHighlight
    }

    fun onHighlight(packet: BlockHighlighting) {
        if (debug) VeinminerClient.LOGGER.info("Received block highlight: $packet")
        if (!packet.allowed) {
            HUDRenderer.updateTarget("forbidden")
            BlockHighlightingRenderer.setShape(emptyList())
            return
        }

        HUDRenderer.updateTarget(packet.icon)
        BlockHighlightingRenderer.setShape(packet.blocks)
    }

    fun onDisconnect() {
        isVeinminerActive = false
        LocalLoopback.reset()
    }


    //
    // Sending packets to server
    //
    fun sendBlockRequest(position: BlockPos, direction: Direction) {
        if (debug) VeinminerClient.LOGGER.info("Sending veinmine request: ($position, $direction)")
        val instance = Minecraft.getInstance()
        instance.connection ?: return notConnected()

        val packet = RequestBlockVein(position.toVeinminer(), direction.toVeinminer(), selectedPattern)
        val bytes = Codec.encode(packet)
        if (shouldSendInternal()) {
            val uuid = instance.player?.uuid ?: return
            NetworkRouter.dispatchC2S(CoreNetworkManager.PACKET_MINE_ID, uuid, bytes)
        } else {
            ClientPlayNetworking.send(VeinminerPayload(mineType, bytes))
        }
    }

    fun sendJoin(version: String) {
        VeinminerClient.LOGGER.info("Sending join: ($version)")
        val instance = Minecraft.getInstance()
        instance.connection ?: return notConnected()

        val payload = JoinInformation(version)
        val bytes = Codec.encode(payload)
        if (shouldSendInternal()) {
            val uuid = instance.player?.uuid ?: return
            LocalLoopback.clientReceiver = loopbackReceiver
            LocalLoopback.loopbackPlayer = uuid
            NetworkRouter.dispatchC2S(CoreNetworkManager.PACKET_JOIN_ID, uuid, bytes)
        } else {
            ClientPlayNetworking.send(VeinminerPayload(joinType, bytes))
        }
    }

    fun sendKeyPress(pressed: Boolean) {
        VeinminerClient.LOGGER.info("Sending veinmine state: $pressed")
        val instance = Minecraft.getInstance()
        instance.connection ?: return notConnected()

        val bytes = Codec.encode(KeyPress(pressed))
        if (shouldSendInternal()) {
            val uuid = instance.player?.uuid ?: return
            NetworkRouter.dispatchC2S(CoreNetworkManager.PACKET_KEY_PRESS_ID, uuid, bytes)
        } else {
            ClientPlayNetworking.send(VeinminerPayload(keyType, bytes))
        }
    }


    //
    // Internal
    //
    private fun notConnected() {
        VeinminerClient.LOGGER.warn("Can not send packet without server connection!")
    }

    private fun shouldSendInternal(): Boolean {
        return VeinminerClient.isSinglePlayer && VeinminerClient.veinminerAvailable
    }
}
