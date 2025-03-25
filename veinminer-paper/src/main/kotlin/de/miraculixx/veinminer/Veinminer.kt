package de.miraculixx.veinminer

import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.UpdateManager
import de.miraculixx.veinminer.networking.PaperNetworking
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.NamespacedKey

class Veinminer : KPaper() {
    companion object {
        lateinit var INSTANCE: Veinminer
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


        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
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

        CoroutineScope(Dispatchers.Default).launch {
            listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_ENCHANTMENT).forEach { module ->
                try {
                    UpdateManager.checkForUpdates(module, "paper", server.minecraftVersion, pluginManager.getPlugin(module.modID)?.pluginMeta?.version)
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