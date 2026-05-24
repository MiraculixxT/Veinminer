@file:Suppress("unused")

package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import de.miraculixx.veinminer.network.ClientCallbacks
import de.miraculixx.veinminer.network.ClientNetworkRouter
import de.miraculixx.veinminer.network.ClientPatternSync
import de.miraculixx.veinminer.network.ClientPlatformNetwork
import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.pattern.PatternConfig
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.config.ClientPatternConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object NetworkManager : ClientCallbacks {
    var isVeinminerActive = false
        private set
    lateinit var selectedPattern: PatternConfig
    var selectedDepth: Int = 6

    var settings: VeinminerSettings = VeinminerSettings()
        private set
    var groups: List<BlockGroup<Identifier>> = emptyList()
        private set
    var veinBlocks: Set<Identifier> = emptySet()
        private set
    var enchantmentActive: Boolean = false
        private set
    var enchantmentKey: Identifier? = null
        private set
    var hostActive: Boolean = true
        private set
    var hasUsePermission: Boolean = true
        private set

    private var initialized = false

    fun init(platform: ClientPlatformNetwork) {
        if (initialized) return
        initialized = true
        ClientNetworkRouter.init(
            platform = platform,
            callbacks = this,
            loopbackPredicate = { ClientLifecycle.isSinglePlayer && ClientLifecycle.veinminerAvailable },
            localPlayerId = { Minecraft.getInstance().player?.uuid }
        )
    }

    override fun onConfiguration(packet: ServerConfiguration) {
        ClientLifecycle.LOGGER.info("Server configuration received (${packet.groups.size} groups, ${packet.veinBlocks.size} blocks)")
        if (packet.settings.debug) ClientLifecycle.LOGGER.info("Configuration packet: $packet")
        if (packet.outdated) {
            Minecraft.getInstance().toastManager.addToast(
                SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("Veinminer Outdated"), Component.literal("Please update Veinminer"))
            )
        }
        isVeinminerActive = true
        settings = packet.settings
        groups = packet.groups.map {
            BlockGroup(
                name = it.name,
                blocks = it.blocks.mapNotNullTo(mutableSetOf(), ::parseId),
                tools = it.tools.mapNotNullTo(mutableSetOf(), ::parseId),
                override = it.override,
            )
        }
        veinBlocks = packet.veinBlocks.mapNotNullTo(mutableSetOf(), ::parseId)
        enchantmentActive = packet.enchantmentActive
        enchantmentKey = packet.enchantmentKey?.let(::parseId)
        hostActive = packet.hostActive
        hasUsePermission = packet.hasUsePermission
    }

    private fun parseId(raw: String): Identifier? = try {
        Identifier.parse(raw)
    } catch (_: Exception) {
        null
    }

    fun onDisconnect() {
        isVeinminerActive = false
        ClientNetworkRouter.onDisconnect()
    }

    fun sendJoin(version: String) {
        if (!isConnected()) return
        ClientLifecycle.LOGGER.info("Sending join: ($version)")
        ClientNetworkRouter.sendJoin(version)
        sendPatternConfig()
    }

    fun sendPatternConfig() {
        if (!canSend()) return
        ClientNetworkRouter.sendPatterns(ClientPatternSync(ClientPatternConfig.settings.patterns))
    }

    fun sendKeyPress(pressed: Boolean, surface: Surface = Surface.UP) {
        if (!isConnected()) return
        if (pressed) sendPatternConfig()
        if (settings.debug) {
            ClientLifecycle.LOGGER.info("Sending veinmine state: $pressed (pattern=${selectedPattern.id} depth=$selectedDepth surface=$surface)")
        }
        ClientNetworkRouter.sendKeyPress(KeyPress(pressed, selectedDepth, surface, selectedPattern.networkId()))
    }

    fun resendKeyPress(surface: Surface = Surface.UP) {
        if (!isConnected()) return
        sendKeyPress(true, surface)
    }

    private fun isConnected(): Boolean {
        if (canSend()) return true
        ClientLifecycle.LOGGER.warn("Can not send packet without server connection!")
        return false
    }

    private fun canSend(): Boolean = Minecraft.getInstance().connection != null

    private fun PatternConfig.networkId(): String? =
        id.takeIf { it.isNotBlank() && it.length <= 64 && it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' || c == '.' || c == ':' } }
}
