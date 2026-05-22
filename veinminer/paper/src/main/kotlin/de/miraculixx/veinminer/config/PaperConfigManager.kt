package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.utils.permissionVeinmine
import kotlinx.serialization.modules.SerializersModule
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import kotlin.io.path.Path

object PaperConfigManager : BaseConfigManager<NamespacedKey>(
    configDir = Path("plugins/Veinminer"),
    serializer = PaperConfigSerializer,
    jsonModule = SerializersModule {
        contextual(NamespacedKey::class, NamespacedKeySerializer)
    },
) {
    override fun onAfterReload() {
        NetworkRouter.registeredPlayers.keys.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach
            val conf = ServerConfiguration(
                outdated = false,
                settings = settings,
                groups = networkGroups,
                veinBlocks = networkVeinBlocks,
                enchantmentActive = Veinminer.enchantmentActive,
                enchantmentKey = Veinminer.VEINMINE.toString(),
                hostActive = Veinminer.INSTANCE.isEnabled,
                hasUsePermission = player.hasPermission(permissionVeinmine),
            )
            NetworkRouter.sendConfiguration(uuid, conf)
        }
        if (settings.debug) Veinminer.LOGGER.info("Sending config to ${NetworkRouter.registeredPlayers.size} players")
    }
}
