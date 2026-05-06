package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.config.BaseConfigManager
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.Enchantment

/**
 * Loader-set state consumed by the common [VeinMinerEvent] algorithm.
 * Each NMS-based loader (Fabric, NeoForge) populates these during its mod-init phase.
 */
object EventState {
    @Volatile
    var enchantmentActive: Boolean = false

    val enchantmentKey: ResourceKey<Enchantment> =
        ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("veinminer-enchantment", "veinminer"))

    lateinit var configManager: BaseConfigManager<Identifier>

    /** Loader plugs in its native permissions API. Defaults to "always allowed". */
    @Volatile
    var checkPermission: (Player, String) -> Boolean = { _, _ -> true }
}
