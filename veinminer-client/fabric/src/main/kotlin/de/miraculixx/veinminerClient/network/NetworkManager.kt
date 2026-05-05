@file:Suppress("unused")

package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.BlockHighlighting
import de.miraculixx.veinminer.network.ClientCallbacks
import de.miraculixx.veinminer.network.ClientNetworkRouter
import de.miraculixx.veinminer.network.RequestBlockVein
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.pattern.Pattern
import de.miraculixx.veinminer.utils.debug
import de.miraculixx.veinminerClient.VeinminerClient
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDRenderer
import de.miraculixx.veinminerClient.utils.toVeinminer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component

object NetworkManager : ClientCallbacks {
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

    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        ClientNetworkRouter.init(
            platform = FabricClientPlatformNetwork,
            callbacks = this,
            loopbackPredicate = { VeinminerClient.isSinglePlayer && VeinminerClient.veinminerAvailable },
            localPlayerId = { Minecraft.getInstance().player?.uuid }
        )
    }

    override fun onConfiguration(packet: ServerConfiguration) {
        VeinminerClient.LOGGER.info("Server configuration: $packet")
        if (packet.outdated) {
            Minecraft.getInstance().toastManager.addToast(
                SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("Veinminer Outdated"), Component.literal("Please update Veinminer"))
            )
        }

        isVeinminerActive = true
        mustSneak = packet.mustSneak
        cooldown = packet.cooldown
        translucentBlockHighlight = packet.translucentBlockHighlight
    }

    override fun onHighlight(packet: BlockHighlighting) {
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
        ClientNetworkRouter.onDisconnect()
    }

    //
    // Sending packets to server
    //
    fun sendBlockRequest(position: BlockPos, direction: Direction) {
        if (debug) VeinminerClient.LOGGER.info("Sending veinmine request: ($position, $direction)")
        if (!isConnected()) return
        ClientNetworkRouter.sendBlockRequest(RequestBlockVein(position.toVeinminer(), direction.toVeinminer(), selectedPattern))
    }

    fun sendJoin(version: String) {
        VeinminerClient.LOGGER.info("Sending join: ($version)")
        if (!isConnected()) return
        ClientNetworkRouter.sendJoin(version)
    }

    fun sendKeyPress(pressed: Boolean) {
        VeinminerClient.LOGGER.info("Sending veinmine state: $pressed")
        if (!isConnected()) return
        ClientNetworkRouter.sendKeyPress(pressed)
    }

    private fun isConnected(): Boolean {
        if (Minecraft.getInstance().connection != null) return true
        VeinminerClient.LOGGER.warn("Can not send packet without server connection!")
        return false
    }
}
