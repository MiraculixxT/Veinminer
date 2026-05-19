package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.ActiveConfig
import de.miraculixx.veinminer.config.ConfigManager
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import me.lucko.fabric.api.permissions.v0.Permissions as FabricPermissions
import net.minecraft.server.permissions.Permissions as VanillaPermissions

object FabricVeinminerCommand {
    fun register() {
        ActiveConfig.bridge = ConfigManager
        Permissions.install { src, node ->
            val css = src as? CommandSourceStack ?: return@install false
            css.isSingleplayerOwner() ||
                FabricPermissions.check(css, node, false) ||
                css.permissions().hasPermission(VanillaPermissions.COMMANDS_GAMEMASTER)
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, registry, _ ->
            dispatcher.register(VeinminerCommand.build(registry))
        }
    }

    private fun CommandSourceStack.isSingleplayerOwner(): Boolean {
        val server = runCatching { server }.getOrNull() ?: return false
        val player = player ?: return false
        return server.isSingleplayerOwner(player.nameAndId())
    }
}
