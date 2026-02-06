package de.miraculixx.veinminerEnchant.paper

import de.miraculixx.veinminer.config.extensions.load
import kotlinx.serialization.Serializable
import kotlin.io.path.Path


@Serializable
data class VeinminerEnchantmentSettings(
    val minCost: Int = 15,
    val maxCost: Int = 65,
    val anvilCost: Int = 7,
    val pickaxeOnly: Boolean = false
) {
    companion object {
        private val filePath = Path("plugins/Veinminer/enchantmentSettings.json")

        fun get(): VeinminerEnchantmentSettings {
            return filePath.load(VeinminerEnchantmentSettings())
        }
    }
}
