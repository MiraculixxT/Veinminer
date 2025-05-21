package de.miraculixx.veinminer

object VeinminerCompatibility {
    private val logger = INSTANCE.logger

    val platform by lazy { checkPlatform().apply { logger.info("Detected platform: $this") } }
    var runsAsync = false

    enum class Platform {
        Bukkit,
        Spigot,
        Paper,
        Folia,
        Purpur
    }

    private fun checkPlatform(): Platform {
        if (runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess)
            return Platform.Folia
        if (runCatching { Class.forName("org.purpurmc.purpur.event.player.PlayerBookTooLargeEvent") }.isSuccess)
            return Platform.Purpur
        if (runCatching { Class.forName("io.papermc.paper.event.player.AbstractChatEvent") }.isSuccess)
            return Platform.Paper
        if (runCatching { Class.forName("org.spigotmc.CustomTimingsHandler") }.isSuccess)
            return Platform.Spigot
        if (runCatching { Class.forName("org.bukkit.Bukkit") }.isSuccess)
            return Platform.Bukkit

        throw IllegalStateException("How did we get here?")
    }

    // Returns false if the plugin should be stopped
    fun isCompatible(): Boolean {
        when (platform) {
            Platform.Bukkit, Platform.Spigot -> {
                logger.severe("Veinminer has been loaded on an installation of $platform!")
                logger.severe("Veinminer does not support anything other than Paper and derivatives!")
                logger.severe("Spigot is considered legacy, and you should definitively consider upgrading!")
                logger.severe("For further information, see https://docs.papermc.io/paper/migration")
                logger.severe("Veinminer will shut down now...")
                return false
            }

            Platform.Paper, Platform.Purpur -> return true
            Platform.Folia -> {
                logger.info("Veinminer running in Folia-compatible mode, please report any issues!")
                runsAsync = true
                return true
            }
        }
    }
}