package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.EnchantmentPack
import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.event.EventState
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.repository.RepositorySource
import net.minecraft.world.flag.FeatureFlagSet
import java.util.Optional
import java.util.function.Consumer

/**
 * Fabric & NeoForge exclusive - injects the generated enchantment datapack as required pack.
 * Regenerated on every pack scan, so changes apply on world (re)load.
 */
object EnchantmentPackSource : RepositorySource {
    private val title = Component.literal("Veinminer Enchantment")
    private val location = PackLocationInfo("veinminer_enchantment", title, PackSource.BUILT_IN, Optional.empty())
    private val metadata = Pack.Metadata(title, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), emptyList())
    private val selection = PackSelectionConfig(true, Pack.Position.TOP, false)

    override fun loadPacks(consumer: Consumer<Pack>) {
        if (!EventState.enchantmentActive) return
        try {
            val root = EnchantmentPack.generate(ConfigManager.configDir)
            consumer.accept(Pack(location, PathPackResources.PathResourcesSupplier(root), metadata, selection))
        } catch (e: Exception) {
            ActiveHost.host.logger.error("Failed to generate enchantment datapack! Veinmine enchantment will be missing", e)
        }
    }
}
