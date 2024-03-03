package de.miraculixx.veinminer

import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig

class Veinminer : KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
        var eventInstance: VeinMinerEvent? = null
    }

    override fun load() {
        INSTANCE = this
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
        ConfigManager
        VeinminerCommand
    }

    override fun startup() {
        CommandAPI.onEnable()
        eventInstance = VeinMinerEvent()
    }

    override fun shutdown() {
        // Soft fix for /reload command. Still not a good idea to use /reload
        CommandAPI.unregister("veinminer")
        CommandAPI.onDisable()
    }
}

val INSTANCE by lazy { Veinminer.INSTANCE }