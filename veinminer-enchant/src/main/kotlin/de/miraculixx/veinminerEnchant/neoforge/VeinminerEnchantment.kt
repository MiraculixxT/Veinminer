package de.miraculixx.veinminerEnchant.neoforge

import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory

@Mod(VeinminerEnchantment.MOD_ID)
class VeinminerEnchantment {
    companion object {
        const val MOD_ID = "veinminer_enchantment"
        val LOGGER = LoggerFactory.getLogger(VeinminerEnchantment::class.java)
    }

    init {
        LOGGER.info("Veinminer_Enchantment loaded (neoforge)")
    }
}
