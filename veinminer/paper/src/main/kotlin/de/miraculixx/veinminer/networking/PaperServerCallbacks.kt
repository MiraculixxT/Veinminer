package de.miraculixx.veinminer.networking

import de.miraculixx.kpaper.event.listen
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.PaperConfigManager
import de.miraculixx.veinminer.network.JoinInformation
import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.ServerCallbacks
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.utils.permissionVeinmine
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

object PaperServerCallbacks : ServerCallbacks {
    @Suppress("unused")
    private val onDisconnect = listen<PlayerQuitEvent> { NetworkRouter.onDisconnect(it.player.uniqueId) }

    override fun onJoinAccepted(playerId: UUID, packet: JoinInformation) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val settings = PaperConfigManager.settings
        if (!settings.client.allow) return

        Veinminer.INSTANCE.logger.info("${player.name} joined with Veinminer version ${packet.veinminerClientVersion}")
        NetworkRouter.registeredPlayers[playerId] = packet.veinminerClientVersion

        val conf = ServerConfiguration(
            outdated = false,
            settings = settings,
            groups = PaperConfigManager.groupsRaw.toList(),
            veinBlocks = PaperConfigManager.veinBlocksRaw.toList(),
            enchantmentActive = Veinminer.enchantmentActive,
            enchantmentKey = Veinminer.VEINMINE.toString(),
            hostActive = Veinminer.INSTANCE.isEnabled,
            hasUsePermission = player.hasPermission(permissionVeinmine),
        )
        NetworkRouter.sendConfiguration(playerId, conf)
    }

    override fun onKeyPress(playerId: UUID, packet: KeyPress) {
        if (ConfigManager.settings.debug) Veinminer.INSTANCE.logger.info("$playerId pressed hotkey (${packet.pressed})")
    }
}
