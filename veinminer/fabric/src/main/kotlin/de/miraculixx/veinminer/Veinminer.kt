@file:Suppress("UnusedExpression")

package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.VeinMinerEvent.removeMiningSpeedModifier
import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.command.FabricVeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.networking.FabricPlatformNetwork
import de.miraculixx.veinminer.networking.FabricServerCallbacks
import de.miraculixx.veinminer.utils.FabricHost
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.mcServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
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
import org.slf4j.Logger
import java.net.URI
import kotlin.jvm.optionals.getOrNull


class Veinminer : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer"
        val LOGGER: Logger = LogUtils.getLogger()
        lateinit var INSTANCE: ModContainer
        var active = true
        val VEINMINE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("veinminer-enchantment", "veinminer"))
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
        ActiveHost.host = FabricHost
        FabricVeinminerCommand.register()
        VeinMinerEvent

        // Networking
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            mcServer = server
            ConfigManager.reload(true) // Load config data
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { _ -> mcServer = null }
        NetworkRouter.init(FabricPlatformNetwork, FabricServerCallbacks)
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            NetworkRouter.onDisconnect(handler.player.uuid)
            handler.player.removeMiningSpeedModifier()
        }

        // Update notification
        ServerPlayConnectionEvents.JOIN.register { packet, _, _ ->
            val player = packet.player
            val info = updateInfo
            val permission = player.server.getProfilePermissions(player.nameAndId())
            if (info != null && (permission.level().id() > 1 || !player.server.isDedicatedServer)) {
                player.sendSystemMessage(
                    Component.literal("${info.module.modID} is outdated! Click here to download the latest version")
                        .setStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI("https://modrinth.com/project/${info.module.modID}"))))
                        .append(" (Current: ").append(Component.literal(info.currentVersion).withColor(cRed))
                        .append(", Latest: ").append(Component.literal(info.latestVersion).withColor(cGreen)).append(")")
                )
            }
        }

        UpdateManager.startUpdateChecker(
            modules = listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_CLIENT),
            platform = "fabric",
            serverVersion = mcVersion,
            moduleVersionLookup = { fabricLoader.getModContainer(it.modID).getOrNull()?.metadata?.version?.friendlyString },
        ) { info -> updateInfo = info }
    }
}
