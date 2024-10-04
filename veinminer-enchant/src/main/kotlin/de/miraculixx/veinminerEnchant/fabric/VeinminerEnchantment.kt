package de.miraculixx.veinminerEnchant.fabric

import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer

class VeinminerEnchantment : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer-enchantment"
        val LOGGER = LogUtils.getLogger()
        lateinit var INSTANCE: ModContainer
    }

    override fun onInitialize() {
        val fabricLoader = FabricLoader.getInstance()
        INSTANCE = fabricLoader.getModContainer(MOD_ID).get()
        LOGGER.info("Veinminer-Enchantment Version: ${INSTANCE.metadata.version} (fabric)")
    }
}
