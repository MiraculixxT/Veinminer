package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.event.EventState
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import java.util.UUID

/**
 * Fabric & NeoForge exclusive - Paper carries its own impl
 */
object ServerCallbacksImpl : ServerCallbacks {
    private val logger get() = ActiveHost.host.logger

    override fun onJoinAccepted(playerId: UUID, packet: JoinInformation) {
        val server = mcServer ?: return logger.warn("Not enough information to handle 'join (no server)' packet!")
        val player = server.playerList.getPlayer(playerId) ?: return logger.warn("Not enough information to handle 'join player' packet!")

        val settings = ConfigManager.settings
        if (!settings.client.allow) return

        logger.info("${player.scoreboardName} joined with Veinminer version ${packet.veinminerClientVersion}")
        NetworkRouter.registeredPlayers[playerId] = packet.veinminerClientVersion

        val conf = ServerConfiguration(
            outdated = false,
            settings = settings,
            groups = ConfigManager.groups.map { group ->
                BlockGroup(
                    name = group.name,
                    blocks = group.blocks.mapTo(mutableSetOf()) { it.toString() },
                    tools = group.tools.mapTo(mutableSetOf()) { it.toString() },
                    override = group.override
                )
            },
            veinBlocks = ConfigManager.veinBlocks.map { it.toString() },
            enchantmentActive = EventState.enchantmentActive,
            enchantmentKey = EventState.enchantmentKey.identifier().toString(),
            hostActive = ActiveHost.host.active,
            hasUsePermission = EventState.checkPermission(player, permissionVeinmine),
        )
        NetworkRouter.sendConfiguration(playerId, conf)
    }

    override fun onKeyPress(playerId: UUID, packet: KeyPress) {
        if (ConfigManager.settings.debug) logger.info("$playerId pressed hotkey (${packet.pressed})")
    }
}
