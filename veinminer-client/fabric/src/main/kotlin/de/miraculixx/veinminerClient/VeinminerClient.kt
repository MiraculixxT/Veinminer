package de.miraculixx.veinminerClient

import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import de.miraculixx.veinminerClient.config.ClientPatternConfig
import de.miraculixx.veinminerClient.constants.FabricKeyBindings
import de.miraculixx.veinminerClient.network.FabricClientPlatformNetwork
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.FabricHUDRenderer
import de.miraculixx.veinminerClient.render.FabricShapeRouletteRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import kotlin.jvm.optionals.getOrNull

class VeinminerClient : ClientModInitializer {

    override fun onInitializeClient() {
        val fabricLoader = FabricLoader.getInstance()
        val instance = fabricLoader.getModContainer(MOD_ID).get()
        val client = Minecraft.getInstance()

        ClientLifecycle.veinminerAvailable = fabricLoader.getModContainer("veinminer").getOrNull() != null

        HUDProvider.instance = FabricHUDRenderer
        ClientPatternConfig.configure(fabricLoader.configDir)
        ClientPatternConfig.load()
        NetworkManager.selectedPattern = ClientPatternConfig.enabledPatterns().first()
        FabricKeyBindings.register()
        NetworkManager.init(FabricClientPlatformNetwork)

        ClientPlayConnectionEvents.JOIN.register { _, _, mc ->
            ClientLifecycle.onJoin(mc, instance.metadata.version.friendlyString)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            ClientLifecycle.onDisconnect()
        }

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            KeyBindManager.tick()
        }

        HudRenderCallback.EVENT.register { graphics, deltaTracker ->
            FabricHUDRenderer.render(graphics, deltaTracker)
            FabricShapeRouletteRenderer.render(graphics, deltaTracker)
        }

        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val stack = context.matrixStack() ?: return@register
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(stack, source, context.camera().position, false)
        }
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            val stack = context.matrixStack() ?: return@register
            val source = Minecraft.getInstance().renderBuffers().bufferSource()
            BlockHighlightingRenderer.render(stack, source, context.camera().position, true)
        }

        mcCoroutineAsync(1.ticks) {
            ClientLifecycle.checkForUpdates(
                "fabric",
                client.launchedVersion,
                fabricLoader.getModContainer("veinminer_client").getOrNull()?.metadata?.version?.friendlyString
            )
        }
    }
}
