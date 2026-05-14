package de.miraculixx.veinminer.networking

import de.miraculixx.kpaper.event.listen
import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.VeinMinerEvent.veinmine
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.PaperConfigManager
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.event.HighlightCache
import de.miraculixx.veinminer.network.BlockHighlighting
import de.miraculixx.veinminer.network.JoinInformation
import de.miraculixx.veinminer.network.KeyPress
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.RequestBlockVein
import de.miraculixx.veinminer.network.ServerCallbacks
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.utils.debug
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

        val conf = ServerConfiguration(settings.cooldown, settings.mustSneak, false, settings.client.translucentBlockHighlight)
        NetworkRouter.sendConfiguration(playerId, conf)
    }

    override fun onKeyPress(playerId: UUID, packet: KeyPress) {
        if (debug) Veinminer.INSTANCE.logger.info("$playerId pressed hotkey (${packet.pressed})")
    }

    override fun onMineRequest(playerId: UUID, packet: RequestBlockVein) {
        val player = Bukkit.getPlayer(playerId) ?: return
        if (debug) Veinminer.INSTANCE.logger.info("$playerId requested to veinmine block at ${packet.blockPosition}")

        NetworkRouter.lastSurface[playerId] = packet.surface

        val pos = packet.blockPosition
        val block = player.world.getBlockAt(pos.x, pos.y, pos.z)
        val action = VeinMinerEvent.allowedToVeinmine(player, block)
        if (action == null) {
            NetworkRouter.sendHighlighting(playerId, BlockHighlighting(false, "", emptyList()))
            return
        }

        val shape = NetworkRouter.activeShape(playerId) ?: Shape.NORMAL
        val maxDepth = NetworkRouter.maxDepth(playerId)
        val cacheKey = HighlightCache.Key(player.world, pos, block.type.key.toString(), shape, packet.surface, maxDepth)
        val cached = HighlightCache.get(cacheKey)
        if (cached != null) {
            NetworkRouter.sendHighlighting(playerId, BlockHighlighting(true, cached.second, cached.first))
            return
        }

        action.copy(settings = action.settings.copy(delay = 0)).veinmine(false)
        val blocks = action.processedBlocks.map { BlockPosition(it.x, it.y, it.z) }
        val toolIcon = VeinMinerEvent.getPreferredToolIcon(block.type)
        HighlightCache.put(cacheKey, blocks, toolIcon)
        NetworkRouter.sendHighlighting(playerId, BlockHighlighting(true, toolIcon, blocks))
    }
}
