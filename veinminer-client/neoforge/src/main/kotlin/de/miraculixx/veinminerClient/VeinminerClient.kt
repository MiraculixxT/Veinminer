package de.miraculixx.veinminerClient

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.UpdateManager
import de.miraculixx.veinminer.extensions.mcCoroutineDelay
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.constants.KeyBindings
import de.miraculixx.veinminerClient.network.NeoForgeClientPlatformNetwork
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.NeoHUDRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.Logger

@Mod(value = VeinminerClient.MOD_ID, dist = [Dist.CLIENT])
class VeinminerClient(modBus: IEventBus, container: ModContainer) {

    companion object {
        const val MOD_ID = "veinminer-client"
        lateinit var client: Minecraft
        val LOGGER: Logger = LogUtils.getLogger()
        var veinminerAvailable = false
        var isSinglePlayer = false
    }

    init {
        client = Minecraft.getInstance()
        veinminerAvailable = ModList.get().isLoaded("veinminer")

        // Initialize client networking (registers payload types + S2C handlers)
        modBus.addListener(NeoForgeClientPlatformNetwork::onRegisterPayloadHandlers)
        NetworkManager.init()

        // Mod-bus registrations: keybinds + HUD layer
        modBus.addListener<RegisterKeyMappingsEvent> { event ->
            KeyBindings.register(event)
        }
        modBus.addListener<RegisterGuiLayersEvent> { event ->
            event.registerAboveAll(Identifier.fromNamespaceAndPath(MOD_ID, "target-info"), NeoHUDRenderer)
        }

        // Game-bus listeners
        val gameBus = NeoForge.EVENT_BUS

        gameBus.addListener<ClientTickEvent.Post> { _ ->
            if (Minecraft.getInstance().level != null) KeyBindManager.tick()
        }

        gameBus.addListener<ClientPlayerNetworkEvent.LoggingIn> { _ ->
            val mc = Minecraft.getInstance()
            isSinglePlayer = mc.singleplayerServer != null
            LOGGER.info("Loading for ${if (isSinglePlayer) "singleplayer" else "multiplayer"}...")
            if (isSinglePlayer && !veinminerAvailable) {
                LOGGER.info("Veinminer not available!")
                KeyBindManager.notifiedOnce = true
                val toast = SystemToast.multiline(
                    mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("veinminer.notavailable.title"),
                    Component.translatable("veinminer.notavailable.subtitle")
                )
                mc.toastManager.addToast(toast)
                return@addListener
            }
            NetworkManager.sendJoin(container.modInfo.version.toString())
        }

        gameBus.addListener<ClientPlayerNetworkEvent.LoggingOut> { _ ->
            NetworkManager.onDisconnect()
            KeyBindManager.onDisconnect()
        }

        // Block-highlight rendering — replaces Fabric's mixin on LevelRenderer.renderBlockOutline.
        // Two passes: solid (after opaque blocks) and translucent (after translucent blocks).
        gameBus.addListener<RenderLevelStageEvent.AfterOpaqueBlocks> { event ->
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(event.poseStack, source, event.levelRenderState.cameraRenderState.pos, false)
        }
        gameBus.addListener<RenderLevelStageEvent.AfterTranslucentBlocks> { event ->
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(event.poseStack, source, event.levelRenderState.cameraRenderState.pos, true)
        }

        // Updater
        mcCoroutineDelay(1.ticks) {
            listOf(UpdateManager.Module.VEINMINER_CLIENT).forEach { module ->
                try {
                    UpdateManager.checkForUpdates(
                        module, "neoforge", client.launchedVersion,
                        ModList.get().getModContainerById(module.modID).orElse(null)?.modInfo?.version?.toString()
                    )
                } catch (e: Exception) {
                    LOGGER.warn("Error while checking for updates: ${e.message}")
                }
            }
        }
    }
}
