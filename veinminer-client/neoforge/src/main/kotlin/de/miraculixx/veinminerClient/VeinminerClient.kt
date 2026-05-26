package de.miraculixx.veinminerClient

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import de.miraculixx.veinminerClient.config.ClientPatternConfig
import de.miraculixx.veinminerClient.config.PatternConfigScreen
import de.miraculixx.veinminerClient.constants.NeoForgeKeyBindings
import de.miraculixx.veinminerClient.network.NeoForgeClientPlatformNetwork
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import de.miraculixx.veinminerClient.render.NeoHUDRenderer
import de.miraculixx.veinminerClient.render.NeoShapeRouletteRenderer
import net.neoforged.neoforge.client.event.InputEvent
import net.minecraft.DetectedVersion
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.common.NeoForge

@Mod(value = MOD_ID, dist = [Dist.CLIENT])
class VeinminerClient(modBus: IEventBus, container: ModContainer) {

    init {
        ClientLifecycle.veinminerAvailable = ModList.get().isLoaded("veinminer")

        HUDProvider.instance = NeoHUDRenderer
        ClientPatternConfig.configure(FMLPaths.CONFIGDIR.get())
        ClientPatternConfig.load()
        NetworkManager.selectedPattern = ClientPatternConfig.enabledPatterns().first()

        container.registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _, parent -> PatternConfigScreen(parent) }
        }

        NetworkManager.init(NeoForgeClientPlatformNetwork)

        modBus.addListener<RegisterKeyMappingsEvent> { event ->
            NeoForgeKeyBindings.register(event)
        }
        modBus.addListener<RegisterGuiLayersEvent> { event ->
            event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MOD_ID, "target-info"), LayeredDraw.Layer(NeoHUDRenderer::render))
            event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MOD_ID, "shape-roulette"), LayeredDraw.Layer(NeoShapeRouletteRenderer::render))
        }

        val gameBus = NeoForge.EVENT_BUS

        gameBus.addListener<ClientTickEvent.Post> { _ ->
            if (Minecraft.getInstance().level != null) KeyBindManager.tick()
        }

        gameBus.addListener<InputEvent.MouseScrollingEvent> { event ->
            if (!KeyBindManager.isPressed) return@addListener
            if (!NetworkManager.isVeinminerActive) return@addListener
            val v = event.scrollDeltaY
            if (v == 0.0) return@addListener
            val w = Minecraft.getInstance().window
            val shift = InputConstants.isKeyDown(w.window, InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(w.window, InputConstants.KEY_RSHIFT)
            KeyBindManager.queueScroll(if (v > 0) 1 else -1, shift)
            event.isCanceled = true
        }

        gameBus.addListener<ClientPlayerNetworkEvent.LoggingIn> { _ ->
            ClientLifecycle.onJoin(Minecraft.getInstance(), container.modInfo.version.toString())
        }

        gameBus.addListener<ClientPlayerNetworkEvent.LoggingOut> { _ ->
            ClientLifecycle.onDisconnect()
        }

        gameBus.addListener<RenderLevelStageEvent> { event ->
            if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return@addListener
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(event.poseStack, source, event.camera.position, false)
        }
        gameBus.addListener<RenderLevelStageEvent> { event ->
            if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return@addListener
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(event.poseStack, source, event.camera.position, true)
        }

        mcCoroutineAsync(1.ticks) {
            ClientLifecycle.checkForUpdates(
                "neoforge",
                DetectedVersion.tryDetectVersion().name,
                ModList.get().getModContainerById("veinminer_client").orElse(null)?.modInfo?.version?.toString()
            )
        }
    }
}
