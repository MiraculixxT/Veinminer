package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.extensions.bukkit.addUrl
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.UpdateManager
import de.miraculixx.veinminer.config.extensions.color
import de.miraculixx.veinminer.config.utils.cGreen
import de.miraculixx.veinminer.config.utils.cRed
import de.miraculixx.veinminer.networking.PaperNetworking
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.NamespacedKey
import org.bukkit.event.player.PlayerJoinEvent

class Veinminer : KPaper() {
    companion object {
        lateinit var INSTANCE: Veinminer
        val LOGGER = LogUtils.getLogger()
        val VEINMINE = NamespacedKey("veinminer-enchantment", "veinminer")
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


        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true).beLenientForMinorVersions(true))
        ConfigManager
        VeinminerCommand
    }

    override fun startup() {
        if (shouldDisable) return // Safeguard because disabling isn't actually instantaneous
        CommandAPI.onEnable()
        VeinMinerEvent
        PaperNetworking

        val enchantmentContainer = server.pluginManager.getPlugin("veinminer-enchantment")
        enchantmentActive = enchantmentContainer != null

        // Update checker
        CoroutineScope(Dispatchers.Default).launch {
            listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_ENCHANTMENT).forEach { module ->
                try {
                    val updateInfo = UpdateManager.checkForUpdates(module, "paper", server.minecraftVersion, pluginManager.getPlugin(module.modID)?.pluginMeta?.version)
                    if (updateInfo.outdated) {
                        // Update notification
                        listen<PlayerJoinEvent> {
                            if (it.player.isOp) {
                                it.player.sendMessage(
                                    cmp("${module.modID} is outdated! Click here to download the latest version").addUrl("https://modrinth.com/project/${module.modID}") +
                                    cmp(" (Current: ") + cmp(updateInfo.currentVersion, cRed.color()) + cmp(", Latest: ") + cmp(updateInfo.latestVersion, cGreen.color()) + cmp(")")
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[VeinminerUpdater] Error while checking for updates: ${e.message}")
                }
            }
        }
    }

    override fun shutdown() {
        if (shouldDisable) return // Safeguard because disabling isn't actually instantaneous
        // Soft fix for /reload command. Still not a good idea to use /reload
        CommandAPI.unregister("veinminer")
        CommandAPI.onDisable()

        // Unregister packet channel
        server.messenger.unregisterOutgoingPluginChannel(this)
        server.messenger.unregisterIncomingPluginChannel(this)
    }

}

val INSTANCE by lazy { Veinminer.INSTANCE }