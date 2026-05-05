@file:Suppress("UnusedExpression")

package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.VeinMinerEvent.removeMiningSpeedModifier
import de.miraculixx.veinminer.command.FabricVeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.networking.FabricPlatformNetwork
import de.miraculixx.veinminer.networking.FabricServerCallbacks
import de.miraculixx.veinminer.utils.mcServer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
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
        val VEINMINE = ResourceKey.create<Enchantment>(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("veinminer-enchantment", "veinminer"))
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
        FabricVeinminerCommand.register()
        VeinMinerEvent

        // Config hook
        Events.Server.preStart.listen {
            ConfigManager.reload(true)
        }

        // Networking
        ServerLifecycleEvents.SERVER_STARTING.register { server -> mcServer = server }
        ServerLifecycleEvents.SERVER_STOPPED.register { _ -> mcServer = null }
        NetworkRouter.init(FabricPlatformNetwork, FabricServerCallbacks)
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            NetworkRouter.onDisconnect(handler.player.uuid)
            handler.player.removeMiningSpeedModifier()
        }

        // Update notification
        PlayerEvents.postLogin.listen(EventPriority.NORMAL, true) { event ->
            val player = event.player
            val info = updateInfo
            val permission = player.server.getProfilePermissions(player.nameAndId())
            if (info != null && (permission.level().id() > 1 || !player.server.isDedicatedServer)) {
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
