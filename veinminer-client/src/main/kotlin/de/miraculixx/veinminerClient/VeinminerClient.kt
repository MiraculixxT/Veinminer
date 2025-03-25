package de.miraculixx.veinminerClient

import com.mojang.logging.LogUtils
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
import org.slf4j.Logger

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

        HudRenderCallback.EVENT.register(HUDRenderer::render)
    }


}