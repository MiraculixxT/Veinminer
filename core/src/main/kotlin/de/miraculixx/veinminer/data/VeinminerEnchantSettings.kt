package de.miraculixx.veinminer.data

import kotlinx.serialization.Serializable

@Serializable
data class VeinminerEnchantSettings(
    var maxLevel: Int = 1,
    var weight: Int = 1,
    var minCost: Int = 15,
    var minCostPerLevel: Int = 0,
    var maxCost: Int = 65,
    var maxCostPerLevel: Int = 0,
    var anvilCost: Int = 7,
    var supportedItems: String = "#minecraft:enchantable/mining",
    var inEnchantingTable: Boolean = true,
    var tradeable: Boolean = true,
    var lootable: Boolean = true,
    var treasure: Boolean = false,
)
