package de.miraculixx.veinminer.networking

import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.VeinMinerEvent.veinmine
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.network.BlockHighlighting
import de.miraculixx.veinminer.network.JoinInformation
import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.RequestBlockVein
import de.miraculixx.veinminer.network.ServerCallbacks
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.utils.debug
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.toNMS
import de.miraculixx.veinminer.utils.toVeinminer
import java.util.UUID

object FabricServerCallbacks : ServerCallbacks {
    override fun onJoinAccepted(playerId: UUID, packet: JoinInformation) {
        val server = mcServer ?: return invalidUserInformation("join (no server)")
        val player = server.playerList.getPlayer(playerId) ?: return invalidUserInformation("join player")

        val settings = ConfigManager.settings
        if (!settings.client.allow) return

        Veinminer.LOGGER.info("${player.scoreboardName} joined with Veinminer version ${packet.veinminerClientVersion}")
        NetworkRouter.registeredPlayers[playerId] = packet.veinminerClientVersion

        val conf = ServerConfiguration(settings.cooldown, settings.mustSneak, false, settings.client.translucentBlockHighlight)
        NetworkRouter.sendConfiguration(playerId, conf)
    }

    override fun onKeyPress(playerId: UUID, packet: KeyPress) {
        if (debug) Veinminer.LOGGER.info("$playerId pressed hotkey (${packet.pressed})")
    }

    override fun onMineRequest(playerId: UUID, packet: RequestBlockVein) {
        val server = mcServer ?: return invalidUserInformation("mine (no server)")
        val player = server.playerList.getPlayer(playerId) ?: return invalidUserInformation("mine player")

        if (debug) Veinminer.LOGGER.info("$playerId requested to veinmine block at ${packet.blockPosition}")

        val level = player.level()
        val position = packet.blockPosition.toNMS()
        val state = level.getBlockState(position)

        val action = VeinMinerEvent.allowedToVeinmine(level, player, position, state)
        if (action == null) {
            NetworkRouter.sendHighlighting(playerId, BlockHighlighting(false, "", emptyList()))
            return
        }

        action.copy(settings = action.settings.copy(delay = 0)).veinmine(false)
        val blocks = action.processedBlocks.map { it.toVeinminer() }
        NetworkRouter.sendHighlighting(playerId, BlockHighlighting(true, VeinMinerEvent.getPreferredToolIcon(state), blocks))
    }

    private fun invalidUserInformation(type: String) {
        Veinminer.LOGGER.warn("Not enough information to handle '$type' packet!")
    }
}
