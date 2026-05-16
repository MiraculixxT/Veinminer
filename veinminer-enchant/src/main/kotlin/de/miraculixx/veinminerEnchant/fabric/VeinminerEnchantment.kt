package de.miraculixx.veinminerEnchant.fabric

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

class VeinminerEnchantment : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer_enchantment"
        val LOGGER = LoggerFactory.getLogger(VeinminerEnchantment::class.java)
    }

    override fun onInitialize() {
        LOGGER.info("Veinminer_Enchantment loaded (fabric)")
    }
}
