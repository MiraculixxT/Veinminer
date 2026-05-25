package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.config.BaseConfigManager
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.enchantment.Enchantment

/**
 * Loader-set state consumed by the common [VeinMinerEvent] algorithm.
 * Each NMS-based loader (Fabric, NeoForge) populates these during its mod-init phase.
 */
object EventState {
    @Volatile
    var enchantmentActive: Boolean = false

    val enchantmentKey: ResourceKey<Enchantment> =
        ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("veinminer_enchantment", "veinminer"))

    lateinit var configManager: BaseConfigManager<Identifier>

    /** Loader plugs in its native permissions API. Defaults to "always allowed". */
    @Volatile
    var checkPermission: (Player, String) -> Boolean = { _, _ -> true }

    /** Loader plugs in native block XP behavior. Fabric uses vanilla; NeoForge has patched block-drop XP hooks. */
    @Volatile
    var dropBlockExperience: (BlockState, ServerLevel, BlockPos, BlockEntity?, Entity?, ItemStack, BlockPos) -> Unit =
        { state, level, _, _, _, tool, dropPos -> state.spawnAfterBreak(level, dropPos, tool, true) }
}
