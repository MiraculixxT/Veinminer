package de.miraculixx.veinminerClient

import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import de.miraculixx.veinminerClient.constants.NeoForgeKeyBindings
import de.miraculixx.veinminerClient.network.NeoForgeClientPlatformNetwork
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import de.miraculixx.veinminerClient.render.NeoHUDRenderer
import net.minecraft.DetectedVersion
import net.minecraft.client.Minecraft
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

@Mod(value = MOD_ID, dist = [Dist.CLIENT])
class VeinminerClient(modBus: IEventBus, container: ModContainer) {

    init {
        ClientLifecycle.veinminerAvailable = ModList.get().isLoaded("veinminer")

        HUDProvider.instance = NeoHUDRenderer

        NetworkManager.init(NeoForgeClientPlatformNetwork)

        modBus.addListener<RegisterKeyMappingsEvent> { event ->
            NeoForgeKeyBindings.register(event)
        }
        modBus.addListener<RegisterGuiLayersEvent> { event ->
            event.registerAboveAll(Identifier.fromNamespaceAndPath(MOD_ID, "target-info"), NeoHUDRenderer)
        }

        val gameBus = NeoForge.EVENT_BUS

        gameBus.addListener<ClientTickEvent.Post> { _ ->
            if (Minecraft.getInstance().level != null) KeyBindManager.tick()
        }

        gameBus.addListener<ClientPlayerNetworkEvent.LoggingIn> { _ ->
            ClientLifecycle.onJoin(Minecraft.getInstance(), container.modInfo.version.toString())
        }

        gameBus.addListener<ClientPlayerNetworkEvent.LoggingOut> { _ ->
            ClientLifecycle.onDisconnect()
        }

        gameBus.addListener<RenderLevelStageEvent.AfterOpaqueBlocks> { event ->
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(event.poseStack, source, event.levelRenderState.cameraRenderState.pos, false)
        }
        gameBus.addListener<RenderLevelStageEvent.AfterTranslucentBlocks> { event ->
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(event.poseStack, source, event.levelRenderState.cameraRenderState.pos, true)
        }

        mcCoroutineAsync(1.ticks) {
            ClientLifecycle.checkForUpdates(
                "neoforge",
                DetectedVersion.tryDetectVersion().name(),
                ModList.get().getModContainerById("veinminer_client").orElse(null)?.modInfo?.version?.toString()
            )
        }
    }
}
