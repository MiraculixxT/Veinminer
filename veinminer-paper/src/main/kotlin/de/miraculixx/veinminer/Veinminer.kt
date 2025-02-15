package de.miraculixx.veinminer

import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.UpdateManager
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.NamespacedKey

class Veinminer : KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
        var eventInstance: VeinMinerEvent? = null
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
        eventInstance = VeinMinerEvent()

        val enchantmentContainer = server.pluginManager.getPlugin("veinminer-enchantment")
        enchantmentActive = enchantmentContainer != null

        CoroutineScope(Dispatchers.Default).launch {
            UpdateManager.Module.entries.forEach { module ->
                try {
                    UpdateManager.checkForUpdates(module, "paper", server.minecraftVersion, pluginManager.getPlugin(module.modID)?.pluginMeta?.version)
                } catch (e: Exception) {
                    println("[VeinminerUpdater] Error while checking for updates: ${e.message}")
                }
            }
        }

        // Register packet channel
//        server.messenger.registerOutgoingPluginChannel(this, IDENTIFIER)
//        server.messenger.registerIncomingPluginChannel(this, IDENTIFIER) { channel, player, message ->
//            if (channel != "veinminer") return@registerIncomingPluginChannel
//            val incoming = ByteStreams.newDataInput(message)
//            val subChannel = incoming.readUTF()
//            println("Sub: $subChannel, Data: ${incoming.readUTF()}")
//
//            when (subChannel) {
//                PACKET_JOIN -> player.sendPacket(PACKET_JOIN_PONG, "pong")
//            }
//        }
    }

    override fun shutdown() {
        if (shouldDisable) return // Safeguard because disabling isn't actually instantaneous
        // Soft fix for /reload command. Still not a good idea to use /reload
        CommandAPI.unregister("veinminer")
        CommandAPI.onDisable()

        // Unregister packet channel
//        server.messenger.unregisterOutgoingPluginChannel(this, IDENTIFIER)
//        server.messenger.unregisterIncomingPluginChannel(this, IDENTIFIER)
    }

//    private fun Player.sendPacket(sub: String, data: String) {
//        val out = ByteStreams.newDataOutput()
//        out.writeUTF(sub)
//        out.writeUTF(data)
//        server.sendPluginMessage(this@Veinminer, IDENTIFIER, out.toByteArray())
//    }

}

val INSTANCE by lazy { Veinminer.INSTANCE }