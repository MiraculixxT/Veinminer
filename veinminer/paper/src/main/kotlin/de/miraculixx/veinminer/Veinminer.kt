@file:Suppress("UnusedExpression")

package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.extensions.bukkit.addUrl
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.command.PaperVeinminerCommand
import de.miraculixx.veinminer.config.PaperConfigManager
import de.miraculixx.veinminer.extensions.color
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.networking.PaperPlatformNetwork
import de.miraculixx.veinminer.networking.PaperServerCallbacks
import de.miraculixx.veinminer.utils.PaperHost
import org.bukkit.NamespacedKey
import org.bukkit.event.player.PlayerJoinEvent

class Veinminer : KPaper() {
    companion object {
        lateinit var INSTANCE: Veinminer
        val LOGGER = LogUtils.getLogger()
        val VEINMINE = NamespacedKey("veinminer_enchantment", "veinminer")
        var enchantmentActive = false
    }

    private var shouldDisable = false

    override fun load() {
        INSTANCE = this
        if (!VeinminerCompatibility.isCompatible()) {
            shouldDisable = true
            pluginManager.disablePlugin(this)
            return
        }

        ActiveHost.host = PaperHost
        ActiveConfig.bridge = PaperConfigManager

        PaperConfigManager
        PaperVeinminerCommand.register()
    }

    override fun startup() {
        if (shouldDisable) return // Safeguard because disabling isn't actually instantaneous
        VeinMinerEvent
        NetworkRouter.init(PaperPlatformNetwork, PaperServerCallbacks)

        val enchantmentContainer = server.pluginManager.getPlugin("veinminer_enchantment")
        enchantmentActive = enchantmentContainer != null

        UpdateManager.startUpdateChecker(
            modules = listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_ENCHANTMENT),
            platform = "paper",
            serverVersion = server.minecraftVersion,
            moduleVersionLookup = { pluginManager.getPlugin(it.modID)?.pluginMeta?.version },
        ) { info ->
            listen<PlayerJoinEvent> {
                if (it.player.isOp) {
                    it.player.sendMessage(
                        cmp("${info.module.modID} is outdated! Click here to download the latest version").addUrl("https://modrinth.com/project/${info.module.modID}") +
                        cmp(" (Current: ") + cmp(info.currentVersion, cRed.color()) + cmp(", Latest: ") + cmp(info.latestVersion, cGreen.color()) + cmp(")")
                    )
                }
            }
        }

        taskRunLater(1) {
            VeinMinerEvent.enabled = true // Combat pesky plugins calling events before server boot
            LOGGER.info("All events enabled!")
        }
    }

    override fun shutdown() {
        if (shouldDisable) return // Safeguard because disabling isn't actually instantaneous

        // Unregister packet channel
        server.messenger.unregisterOutgoingPluginChannel(this)
        server.messenger.unregisterIncomingPluginChannel(this)
    }

}

val INSTANCE by lazy { Veinminer.INSTANCE }