package de.miraculixx.veinminerClient

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.UpdateManager
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import org.slf4j.Logger

object ClientLifecycle {
    const val MOD_ID = "veinminer_client"
    val LOGGER: Logger = LogUtils.getLogger()
    var veinminerAvailable = false
    var isSinglePlayer = false

    fun onJoin(mc: Minecraft, version: String) {
        isSinglePlayer = mc.singleplayerServer != null
        LOGGER.info("Loading for ${if (isSinglePlayer) "singleplayer" else "multiplayer"}...")
        if (isSinglePlayer && !veinminerAvailable) {
            LOGGER.info("Veinminer not available!")
            KeyBindManager.notifiedOnce = true
            SystemToast.add(
                mc.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("veinminer.notavailable.title"),
                Component.translatable("veinminer.notavailable.subtitle")
            )
            return
        }
        NetworkManager.sendJoin(version)
    }

    fun onDisconnect() {
        NetworkManager.onDisconnect()
        KeyBindManager.onDisconnect()
    }

    fun checkForUpdates(loader: String, mcVersion: String, modVersion: String?) {
        try {
            UpdateManager.checkForUpdates(UpdateManager.Module.VEINMINER_CLIENT, loader, mcVersion, modVersion)
        } catch (e: Exception) {
            LOGGER.warn("Error while checking for updates: ${e.message}")
        }
    }
}
