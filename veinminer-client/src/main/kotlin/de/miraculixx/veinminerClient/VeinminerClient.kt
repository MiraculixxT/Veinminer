package de.miraculixx.veinminerClient

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.config.UpdateManager
import de.miraculixx.veinminerClient.constants.KEY_VEINMINE
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.HUDRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.silkmc.silk.core.task.mcCoroutineTask
import org.slf4j.Logger
import kotlin.jvm.optionals.getOrNull

class VeinminerClient : ClientModInitializer {

    companion object {
        const val MOD_ID = "veinminer-client"
        lateinit var client: Minecraft
        val LOGGER: Logger = LogUtils.getLogger()
    }

    override fun onInitializeClient() {
        val fabricLoader = FabricLoader.getInstance()
        val instance = fabricLoader.getModContainer(MOD_ID).get()
        client = Minecraft.getInstance()

        // Register keybinds
        KEY_VEINMINE

        ClientPlayConnectionEvents.JOIN.register { packet, sender, mc ->
            // Inform the server that we are ready to receive the configuration
            NetworkManager.sendJoin(instance.metadata.version.friendlyString)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            NetworkManager.onDisconnect()
            KeyBindManager.onDisconnect()
        }

        WorldRenderEvents.AFTER_TRANSLUCENT.register(BlockHighlightingRenderer::render)
        ClientTickEvents.END_CLIENT_TICK.register {
            KeyBindManager.tick()
        }

        // Use for backwards compatibility
        HudRenderCallback.EVENT.register(HUDRenderer::render)

        // Updater
        mcCoroutineTask(false) {
            listOf(UpdateManager.Module.VEINMINER_CLIENT).forEach { module ->
                try {
                    UpdateManager.checkForUpdates(module, "fabric", client.launchedVersion ?: "1.21.4", fabricLoader.getModContainer(module.modID).getOrNull()?.metadata?.version?.friendlyString)
                } catch (e: Exception) { LOGGER.warn("[VeinminerUpdater] Error while checking for updates: ${e.message}") }
            }
        }
    }


}