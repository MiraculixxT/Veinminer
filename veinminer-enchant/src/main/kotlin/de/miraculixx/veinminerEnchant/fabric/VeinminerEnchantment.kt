package de.miraculixx.veinminerEnchant.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import java.util.logging.Logger

class VeinminerEnchantment : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer-enchantment"
        val LOGGER: Logger = Logger.getLogger(MOD_ID)
        lateinit var INSTANCE: ModContainer
    }

    override fun onInitialize() {
        val fabricLoader = FabricLoader.getInstance()
        INSTANCE = fabricLoader.getModContainer(MOD_ID).get()
        LOGGER.info("Veinminer-Enchantment Version: ${INSTANCE.metadata.version} (fabric)")
    }
}
