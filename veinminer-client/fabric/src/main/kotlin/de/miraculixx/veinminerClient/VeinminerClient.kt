package de.miraculixx.veinminerClient

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.config.UpdateManager
import de.miraculixx.veinminerClient.constants.KEY_VEINMINE_HOLD
import de.miraculixx.veinminerClient.constants.KEY_VEINMINE_TOGGLE
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.HUDRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.silkmc.silk.core.task.mcCoroutineTask
import org.slf4j.Logger
import kotlin.jvm.optionals.getOrNull

class VeinminerClient : ClientModInitializer {

    companion object {
        const val MOD_ID = "veinminer-client"
        lateinit var client: Minecraft
        val LOGGER: Logger = LogUtils.getLogger()
        var veinminerAvailable = false
        var isSinglePlayer = false
    }

    override fun onInitializeClient() {
        val fabricLoader = FabricLoader.getInstance()
        val instance = fabricLoader.getModContainer(MOD_ID).get()
        client = Minecraft.getInstance()

        // Check if Veinminer is available
        val veinminerContainer = fabricLoader.getModContainer("veinminer").getOrNull()
        if (veinminerContainer != null) veinminerAvailable = true

        // Register keybinds
        KEY_VEINMINE_TOGGLE
        KEY_VEINMINE_HOLD

        ClientPlayConnectionEvents.JOIN.register { packet, sender, mc ->
            isSinglePlayer = mc.singleplayerServer != null
            LOGGER.info("Loading for ${if (isSinglePlayer) "singleplayer" else "multiplayer"}...")
            if (isSinglePlayer && !veinminerAvailable) {
                LOGGER.info("Veinminer not available!")
                KeyBindManager.notifiedOnce = true
                val toast = SystemToast.multiline(mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("veinminer.notavailable.title"),
                    Component.translatable("veinminer.notavailable.subtitle")
                )
                mc.toastManager.addToast(toast)
                return@register
            }

            // Inform the server that we are ready to receive the configuration
            NetworkManager.sendJoin(instance.metadata.version.friendlyString)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            NetworkManager.onDisconnect()
            KeyBindManager.onDisconnect()
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            KeyBindManager.tick()
        }

        // Crosshair info
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "target-info"), HUDRenderer)

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