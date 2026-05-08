package de.miraculixx.veinminerClient

import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import de.miraculixx.veinminerClient.constants.FabricKeyBindings
import de.miraculixx.veinminerClient.network.FabricClientPlatformNetwork
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.FabricHUDRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import kotlin.jvm.optionals.getOrNull

class VeinminerClient : ClientModInitializer {

    override fun onInitializeClient() {
        val fabricLoader = FabricLoader.getInstance()
        val instance = fabricLoader.getModContainer(MOD_ID).get()
        val client = Minecraft.getInstance()

        ClientLifecycle.veinminerAvailable = fabricLoader.getModContainer("veinminer").getOrNull() != null

        HUDProvider.instance = FabricHUDRenderer
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

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "target-info"), FabricHUDRenderer)

        mcCoroutineAsync(1.ticks) {
            ClientLifecycle.checkForUpdates(
                "fabric",
                client.launchedVersion,
                fabricLoader.getModContainer("veinminer_client").getOrNull()?.metadata?.version?.friendlyString
            )
        }
    }
}
