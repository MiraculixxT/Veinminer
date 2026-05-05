package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.ActiveConfig
import de.miraculixx.veinminer.config.ConfigManager
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.PermissionLevel
import me.lucko.fabric.api.permissions.v0.Permissions as FabricPermissions

object FabricVeinminerCommand {
    fun register() {
        ActiveConfig.bridge = ConfigManager
        Permissions.install { src, node ->
            val css = src as? CommandSourceStack ?: return@install false
            FabricPermissions.require(node, PermissionLevel.GAMEMASTERS).test(css)
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, registry, _ ->
            dispatcher.register(VeinminerCommand.build(registry))
        }
    }
}
