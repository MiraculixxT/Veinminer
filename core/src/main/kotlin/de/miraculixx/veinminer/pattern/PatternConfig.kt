package de.miraculixx.veinminer.pattern

import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

@Serializable
enum class PatternType {
    NORMAL,
    FLAT,
    TUNNEL,
    STAIRS
}

@Serializable
data class PatternConfig(
    val id: String,
    var enabled: Boolean = true,
    var type: PatternType = PatternType.NORMAL,
    var color: Int = 0x73D45C,
    var width: Int = 1,
    var height: Int = 1,
    var stairsUp: Boolean = true,
) {
    fun strategy(): ShapeStrategy = when (type) {
        PatternType.NORMAL -> NormalStrategy
        PatternType.FLAT -> FlatStrategy
        PatternType.TUNNEL -> TunnelStrategy(width.coerceAtLeast(1), height.coerceAtLeast(1))
        PatternType.STAIRS -> StairsStrategy(stairsUp, width.coerceAtLeast(1), height.coerceAtLeast(1))
    }

    fun icon(): ResourceLocation {
        val icon = when (type) {
            PatternType.NORMAL -> "normal"
            PatternType.FLAT -> "flat"
            PatternType.TUNNEL -> "tunnel_${width.coerceIn(1, 3)}x${height.coerceIn(1, 3)}"
            PatternType.STAIRS -> "stairs_${if (stairsUp) "up" else "down"}"
        }
        return ResourceLocation.fromNamespaceAndPath("veinminer_client", "textures/gui/sprites/shape/$icon.png")
    }
}

object DefaultPatterns {
    fun all(): MutableList<PatternConfig> = mutableListOf(
        PatternConfig(
            id = "normal",
            type = PatternType.NORMAL,
            color = 0x73D45C,
        ),
        PatternConfig(
            id = "flat",
            type = PatternType.FLAT,
            color = 0x6FB7F0,
        ),
        PatternConfig(
            id = "tunnel_1x1",
            type = PatternType.TUNNEL,
            color = 0xF0C56E,
            width = 1,
            height = 1,
        ),
        PatternConfig(
            id = "tunnel_2x2",
            type = PatternType.TUNNEL,
            color = 0xF09E4A,
            width = 2,
            height = 2,
        ),
        PatternConfig(
            id = "tunnel_3x3",
            type = PatternType.TUNNEL,
            color = 0xE87434,
            width = 3,
            height = 3,
        ),
        PatternConfig(
            id = "stairs_up_1x3",
            type = PatternType.STAIRS,
            color = 0xB48CF0,
            width = 1,
            height = 3,
            stairsUp = true
        ),
        PatternConfig(
            id = "stairs_down_1x3",
            type = PatternType.STAIRS,
            color = 0x70519E,
            width = 1,
            height = 3,
            stairsUp = false
        ),
    )
}
