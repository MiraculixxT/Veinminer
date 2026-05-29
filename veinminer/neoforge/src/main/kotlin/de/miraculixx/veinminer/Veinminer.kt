package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.command.NeoForgeVeinminerCommand
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.event.EventState
import de.miraculixx.veinminer.event.VeinMinerEvent
import de.miraculixx.veinminer.event.VeinMinerEvent.removeMiningSpeedModifier
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.ServerCallbacksImpl
import de.miraculixx.veinminer.networking.NeoForgePlatformNetwork
import de.miraculixx.veinminer.utils.NeoForgeHost
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.mcServer
import net.minecraft.DetectedVersion
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import org.slf4j.Logger

@Mod(Veinminer.MOD_ID)
class Veinminer(modBus: IEventBus, container: ModContainer) {
    companion object {
        const val MOD_ID = "veinminer"
        val LOGGER: Logger = LogUtils.getLogger()
        lateinit var INSTANCE: ModContainer
        var active = true
        var updateInfo: UpdateManager.VersionInfo? = null
    }

    init {
        INSTANCE = container
        LOGGER.info("Veinminer Version: ${container.modInfo.version} (neoforge)")
        val mcVersion = DetectedVersion.tryDetectVersion().name()

        // Check for Veinminer-Enchantment
        EventState.enchantmentActive = ModList.get().isLoaded("veinminer_enchantment")

        // Registration
        ActiveHost.host = NeoForgeHost
        EventState.configManager = ConfigManager
        EventState.checkPermission = { _, _ -> true } // NeoForge has no permissions API; op-level checks live in command layer
        EventState.dropBlockExperience = { state, level, blockPos, blockEntity, breaker, tool, dropPos ->
            state.spawnAfterBreak(level, dropPos, tool, false)
            val experience = EnchantmentHelper.processBlockExperience(
                level, tool,
                state.getExpDrop(level, blockPos, blockEntity, breaker, tool)
            )
            if (experience > 0) ExperienceOrb.award(level, Vec3.atCenterOf(dropPos), experience)
        }

        val gameBus = NeoForge.EVENT_BUS

        // Block-attack speed modifier
        gameBus.addListener<PlayerInteractEvent.LeftClickBlock> { event ->
            val world = event.level
            if (!world.isClientSide) {
                val pos = event.pos
                val state = world.getBlockState(pos)
                VeinMinerEvent.applySpeedModifierOnAttack(world, event.entity, pos, state)
            }
        }

        // Block break
        gameBus.addListener<BlockEvent.BreakEvent> { event ->
            val player = event.player
            val world = player.level()
            if (world.isClientSide) return@addListener
            val proceed = VeinMinerEvent.onBlockBreakBefore(world, player, event.pos, event.state)
            if (!proceed) event.setCanceled(true)
        }

        // Commands
        gameBus.addListener<RegisterCommandsEvent> { event ->
            NeoForgeVeinminerCommand.register(event.dispatcher, event.buildContext)
        }

        // Server lifecycle / network init
        gameBus.addListener<ServerStartingEvent> { event ->
            mcServer = event.server
            ConfigManager.reload(true)
        }
        gameBus.addListener<ServerStoppedEvent> { _ ->
            mcServer = null
        }

        // Network registrar (mod bus event)
        modBus.addListener(NeoForgePlatformNetwork::onRegisterPayloadHandlers)

        NetworkRouter.init(NeoForgePlatformNetwork, ServerCallbacksImpl)

        // Disconnect cleanup
        gameBus.addListener<PlayerEvent.PlayerLoggedOutEvent> { event ->
            val player = event.entity
            NetworkRouter.onDisconnect(player.uuid)
            player.removeMiningSpeedModifier()
        }

        // Update notification on join
        gameBus.addListener<PlayerEvent.PlayerLoggedInEvent> { event ->
            val player = event.entity as? ServerPlayer ?: return@addListener
            val info = updateInfo ?: return@addListener
            val server = player.server
            val isOp = server.playerList.isOp(player.nameAndId())
            if (isOp || !server.isDedicatedServer) {
                player.sendSystemMessage(
                    Component.literal("${info.module.modID} is outdated! ")
                        .append(" (Current: ").append(Component.literal(info.currentVersion).withColor(cRed))
                        .append(", Latest: ").append(Component.literal(info.latestVersion).withColor(cGreen)).append(")")
                        .append("\nDownload: ").append(VeinminerCommand.link("Modrinth", "https://modrinth.com/mod/${info.module.modID}"))
                        .append(" | ").append(VeinminerCommand.link("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/${info.module.cfID}"))
                )
            }
        }

        UpdateManager.startUpdateChecker(
            modules = listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_CLIENT),
            platform = "neoforge",
            serverVersion = mcVersion,
            moduleVersionLookup = { ModList.get().getModContainerById(it.modID).orElse(null)?.modInfo?.version?.toString() },
        ) { info -> updateInfo = info }
    }
}
