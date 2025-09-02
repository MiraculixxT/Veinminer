package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.UpdateManager
import de.miraculixx.veinminer.config.utils.cGreen
import de.miraculixx.veinminer.config.utils.cRed
import de.miraculixx.veinminer.networking.FabricNetworking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.enchantment.Enchantment
import net.silkmc.silk.core.event.EventPriority
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.PlayerEvents
import net.silkmc.silk.core.event.Server
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.sendText
import java.net.URI
import kotlin.jvm.optionals.getOrNull


class Veinminer : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer"
        val LOGGER = LogUtils.getLogger()
        lateinit var INSTANCE: ModContainer
        var active = true
        val VEINMINE = ResourceKey.create<Enchantment>(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("veinminer-enchantment", "veinminer"))
        var enchantmentActive = false
        var updateInfo: UpdateManager.VersionInfo? = null
    }

    private lateinit var fabricLoader: FabricLoader

    @Suppress("OPT_IN_USAGE")
    override fun onInitialize() {
        // Initializing
        fabricLoader = FabricLoader.getInstance()
        INSTANCE = fabricLoader.getModContainer(MOD_ID).get()
        LOGGER.info("Veinminer Version: ${INSTANCE.metadata.version} (fabric)")
        val mcVersion = (FabricLoader.getInstance() as FabricLoaderImpl).gameProvider.rawGameVersion

        // Check for Veinminer-Enchantment
        val enchantmentContainer = fabricLoader.getModContainer("veinminer-enchantment").getOrNull()
        enchantmentActive = enchantmentContainer != null

        // Registration
        VeinminerCommand
        VeinMinerEvent

        // Config hook
        Events.Server.preStart.listen {
            ConfigManager.reload(true)
        }

        // Networking
        ServerPlayConnectionEvents.DISCONNECT.register(FabricNetworking::onDisconnect)

        // Update notification
        PlayerEvents.postLogin.listen(EventPriority.NORMAL, true) { event ->
            val player = event.player
            val info = updateInfo
            if (info != null && (player.permissionLevel > 1 || player.server?.isDedicatedServer == false)) {
                player.sendText(
                    Component.literal("${info.module.modID} is outdated! Click here to download the latest version")
                        .setStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI("https://modrinth.com/project/${info.module.modID}"))))
                        .append(" (Current: ").append(Component.literal(info.currentVersion).withColor(cRed))
                        .append(", Latest: ").append(Component.literal(info.latestVersion).withColor(cGreen)).append(")")
                )
            }
        }

        // Updater
        mcCoroutineTask(false) {
            listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_CLIENT).forEach { module ->
                try {
                    val info = UpdateManager.checkForUpdates(module, "fabric", mcVersion, fabricLoader.getModContainer(module.modID).getOrNull()?.metadata?.version?.friendlyString)
                    if (info.outdated) updateInfo = info
                } catch (e: Exception) { println("[VeinminerUpdater] Error while checking for updates: ${e.message}") }
            }
        }
    }
}
