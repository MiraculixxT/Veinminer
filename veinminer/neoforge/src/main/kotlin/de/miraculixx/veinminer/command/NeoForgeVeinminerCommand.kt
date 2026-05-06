package de.miraculixx.veinminer.command

import com.mojang.brigadier.CommandDispatcher
import de.miraculixx.veinminer.ActiveConfig
import de.miraculixx.veinminer.config.ConfigManager
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel

object NeoForgeVeinminerCommand {
    private val gamemasters = Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, ctx: CommandBuildContext) {
        ActiveConfig.bridge = ConfigManager
        Permissions.install { src, _ ->
            val css = src as? CommandSourceStack ?: return@install false
            // NeoForge has no permissions API??? Gate at op level 2 (gamemasters)
            css.permissions().hasPermission(gamemasters)
        }
        dispatcher.register(VeinminerCommand.build(ctx))
    }
}
