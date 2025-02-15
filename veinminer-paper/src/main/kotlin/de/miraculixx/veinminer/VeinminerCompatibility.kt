package de.miraculixx.veinminer

import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.veinminer.config.ConfigManager
import net.kyori.adventure.text.Component

object VeinminerCompatibility {
    private val logger = INSTANCE.logger

    val platform by lazy { checkPlatform() }

    enum class Platform {
        Bukkit,
        Spigot,
        Paper,
        Folia,
        Purpur
    }

    private fun checkPlatform(): Platform {
        var platform = Platform.Folia
        if (runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isFailure)
            platform = Platform.Purpur
        if (runCatching { Class.forName("org.purpurmc.purpur.event.player.PlayerBookTooLargeEvent") }.isFailure)
            platform = Platform.Paper
        if (runCatching { Class.forName("io.papermc.paper.event.player.AbstractChatEvent") }.isFailure)
            platform = Platform.Spigot
        if (runCatching { Class.forName("org.spigotmc.CustomTimingsHandler") }.isFailure)
            platform = Platform.Bukkit
        if (runCatching { Class.forName("org.bukkit.Bukkit") }.isFailure)
            throw IllegalStateException("How did we get here?")

        return platform
    }

    // Returns false if the plugin should be stopped
    fun isCompatible(): Boolean {
        when (platform) {
            Platform.Bukkit, Platform.Spigot -> {
                logger.severe("Veinminer has been loaded on an installation of Bukkit/Spigot!")
                logger.severe("Veinminer does not support anything other than Paper and derivatives!")
                logger.severe("Spigot is considered legacy, and you should definitively consider upgrading!")
                logger.severe("For further information, see https://docs.papermc.io/paper/migration")
                logger.severe("Veinminer will shut down now...")
                return false
            }
            Platform.Paper, Platform.Purpur -> return true
            Platform.Folia -> {
                logger.info("Veinminer running in Folia-compatible mode")
                ConfigManager.settings.delay = 0
                return true
            }
        }
    }
}