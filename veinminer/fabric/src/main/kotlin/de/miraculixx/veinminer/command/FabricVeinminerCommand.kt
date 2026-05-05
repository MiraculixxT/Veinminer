package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.ActiveConfig
import de.miraculixx.veinminer.config.ConfigManager
import me.lucko.fabric.api.permissions.v0.Permissions as FabricPermissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.DetectedVersion
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.PermissionLevel
import org.slf4j.Logger

private object FabricHost : VeinminerHost {
    override val versionVeinminer: String = Veinminer.INSTANCE.metadata.version.friendlyString
    override val versionMinecraft: String = DetectedVersion.tryDetectVersion().name()
    override val platform: String = "Fabric"
    override val logger: Logger = Veinminer.LOGGER

    override var active: Boolean
        get() = Veinminer.active
        set(value) {
            Veinminer.active = value
        }
}

object FabricVeinminerCommand {
    fun register() {
        ActiveConfig.bridge = ConfigManager
        ActiveHost.host = FabricHost
        Permissions.install { src, node ->
            val css = src as? CommandSourceStack ?: return@install false
            FabricPermissions.require(node, PermissionLevel.GAMEMASTERS).test(css)
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, registry, _ ->
            dispatcher.register(VeinminerCommand.build(registry))
        }
    }
}
