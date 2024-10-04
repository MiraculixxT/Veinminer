package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.UpdateManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.enchantment.Enchantment
import net.silkmc.silk.core.Silk.server
import net.silkmc.silk.core.event.PlayerEvents
import net.silkmc.silk.core.task.mcCoroutineTask
import kotlin.jvm.optionals.getOrNull


class Veinminer : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer"
        val LOGGER = LogUtils.getLogger()
        lateinit var INSTANCE: ModContainer
        var active = true
        val VEINMINE = ResourceKey.create<Enchantment>(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("veinminer-enchantment", "veinminer"))
        var enchantmentActive = false
    }

    private lateinit var fabricLoader: FabricLoader

    override fun onInitialize() {
        fabricLoader = FabricLoader.getInstance()
        INSTANCE = fabricLoader.getModContainer(MOD_ID).get()
        LOGGER.info("Veinminer Version: ${INSTANCE.metadata.version} (fabric)")

        val enchantmentContainer = fabricLoader.getModContainer("veinminer-enchantment").getOrNull()
        enchantmentActive = enchantmentContainer != null

        VeinminerCommand

        mcCoroutineTask(false) {
            UpdateManager.Module.entries.forEach { module ->
                try {
                    UpdateManager.checkForUpdates(module, "fabric", server?.serverVersion ?: "1.21", fabricLoader.getModContainer(module.modID).getOrNull()?.metadata?.version?.friendlyString)
                } catch (e: Exception) { println("[VeinminerUpdater] Error while checking for updates: ${e.message}") }
            }
        }

        VeinMinerEvent


    }
}
