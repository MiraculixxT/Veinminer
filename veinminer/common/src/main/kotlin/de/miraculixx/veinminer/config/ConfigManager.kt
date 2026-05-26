package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.event.EventState
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.resources.ResourceLocation
import kotlin.io.path.Path

/**
 * Fabric & NeoForge exclusive - Paper carries its own impl
 */
object ConfigManager : BaseConfigManager<ResourceLocation>(
    configDir = Path("config/Veinminer"),
    serializer = IdentifierConfigSerializer,
    jsonModule = SerializersModule {
        contextual(ResourceLocation::class, ResourceLocationSerializer)
    }
) {
    @Suppress("USELESS_ELVIS")
    override fun onAfterReload() {
        val server = mcServer ?: return
        val playerList = server.playerList ?: return ActiveHost.host.logger.warn("No player list available!") // fabric can be null
        NetworkRouter.registeredPlayers.keys.forEach { uuid ->
            val player = playerList.getPlayer(uuid) ?: return@forEach
            val conf = ServerConfiguration(
                outdated = false,
                settings = settings,
                groups = networkGroups,
                veinBlocks = networkVeinBlocks,
                enchantmentActive = EventState.enchantmentActive,
                enchantmentKey = EventState.enchantmentKey.location().toString(),
                hostActive = ActiveHost.host.active,
                hasUsePermission = EventState.checkPermission(player, permissionVeinmine),
            )
            NetworkRouter.sendConfiguration(uuid, conf)
        }
        if (settings.debug) ActiveHost.host.logger.info("Sending config to ${NetworkRouter.registeredPlayers.size} players")
    }
}
