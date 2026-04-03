package de.miraculixx.veinminerEnchant.paper

import com.google.gson.GsonBuilder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class VeinminerEnchantmentSettings(
    val minCost: Int = 15,
    val maxCost: Int = 65,
    val anvilCost: Int = 7,
    val pickaxeOnly: Boolean = false
) {
    companion object {
        private val filePath: Path = Path("plugins/Veinminer/enchantmentSettings.json")
        private val gson = GsonBuilder().setPrettyPrinting().create()

        private data class RawSettings(
            val minCost: Int? = null,
            val maxCost: Int? = null,
            val anvilCost: Int? = null,
            val pickaxeOnly: Boolean? = null
        )

        fun get(): VeinminerEnchantmentSettings {
            val defaults = VeinminerEnchantmentSettings()

            return try {
                if (!filePath.exists()) {
                    filePath.parent?.createDirectories()
                    filePath.writeText(gson.toJson(defaults))
                    println("[VeinminerEnchant] Created ${filePath.fileName} default config")
                    defaults
                } else {
                    val raw = gson.fromJson(filePath.readText(), RawSettings::class.java)
                    VeinminerEnchantmentSettings(
                        minCost = raw?.minCost ?: defaults.minCost,
                        maxCost = raw?.maxCost ?: defaults.maxCost,
                        anvilCost = raw?.anvilCost ?: defaults.anvilCost,
                        pickaxeOnly = raw?.pickaxeOnly ?: defaults.pickaxeOnly
                    )
                }
            } catch (e: Exception) {
                println("[VeinminerEnchant] Failed to load ${filePath.fileName} config: Reason: ${e.message}")
                defaults
            }
        }
    }
}
