@file:Suppress("unused")

package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.config.network.BlockHighlighting
import de.miraculixx.veinminer.config.network.JoinInformation
import de.miraculixx.veinminer.config.network.KeyPress
import de.miraculixx.veinminer.config.network.RequestBlockVein
import de.miraculixx.veinminer.config.network.ServerConfiguration
import de.miraculixx.veinminer.config.pattern.Pattern
import de.miraculixx.veinminer.config.utils.debug
import de.miraculixx.veinminer.networking.FabricNetworking
import de.miraculixx.veinminer.networking.PACKET_CONFIGURATION
import de.miraculixx.veinminer.networking.PACKET_HIGHLIGHT
import de.miraculixx.veinminer.networking.PACKET_JOIN
import de.miraculixx.veinminer.networking.PACKET_KEY_PRESS
import de.miraculixx.veinminer.networking.PACKET_MINE
import de.miraculixx.veinminerClient.VeinminerClient
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDRenderer
import de.miraculixx.veinminerClient.utils.toVeinminer
import net.fabricmc.fabric.impl.networking.RegistrationPayload
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket

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


    //
    // Receiving packets from server
    //
    private val onConfiguration = PACKET_CONFIGURATION.receiveOnClient { packet, context -> onConfiguration(packet, context.client) }
    private val onHighlight = PACKET_HIGHLIGHT.receiveOnClient { packet, context -> onHighlight(packet) }

    // Move to internal communication, skipping packets
    private val localDispatch = object : FabricNetworking.LocalDispatch {
        override fun onConfiguration(packet: ServerConfiguration) {
            this@NetworkManager.onConfiguration(packet, Minecraft.getInstance())
        }
        override fun onHighlight(packet: BlockHighlighting) {
            this@NetworkManager.onHighlight(packet)
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
        FabricNetworking.localDispatch = null
    }


    //
    // Sending packets to server
    //
    fun sendBlockRequest(position: BlockPos, direction: Direction) {
        if (debug) VeinminerClient.LOGGER.info("Sending veinmine request: ($position, $direction)")
        val instance = Minecraft.getInstance()
        instance.connection ?: return notConnected()

        val packet = RequestBlockVein(position.toVeinminer(), direction.toVeinminer(), selectedPattern)
        if (shouldSendInternal()) FabricNetworking.onMine(null, packet, instance.player?.uuid)
        else PACKET_MINE.send(packet)
    }

    fun sendJoin(version: String) {
        VeinminerClient.LOGGER.info("Sending join: ($version)")

        val instance = Minecraft.getInstance()
        val con = instance.connection ?: return notConnected()

        if (shouldSendInternal()) {
            // Direct wiring for singleplayer, skipping the networking layer
            FabricNetworking.localDispatch = localDispatch
            FabricNetworking.onJoin(null, JoinInformation(version), instance.player?.uuid)
        } else {
            // Register incoming packets on the server side of the connection
            con.send(ServerboundCustomPayloadPacket(
                RegistrationPayload(RegistrationPayload.REGISTER, listOf(PACKET_CONFIGURATION.id, PACKET_HIGHLIGHT.id))
            ))
            PACKET_JOIN.send(JoinInformation(version))
        }
    }

    fun sendKeyPress(pressed: Boolean) {
        VeinminerClient.LOGGER.info("Sending veinmine state: $pressed")
        val instance = Minecraft.getInstance()
        instance.connection ?: return notConnected()

        if (shouldSendInternal()) FabricNetworking.onPress(instance.player?.uuid, KeyPress(pressed))
        else PACKET_KEY_PRESS.send(KeyPress(pressed))
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