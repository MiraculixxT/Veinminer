package de.miraculixx.veinminer.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack

inline fun cmd(
    name: String,
    crossinline builder: LiteralArgumentBuilder<CommandSourceStack>.() -> Unit,
) {
    CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
        dispatcher.register(LiteralArgumentBuilder.literal<CommandSourceStack?>(name).apply(builder))
    })
}

fun LiteralArgumentBuilder<CommandSourceStack>.literal(name: String)
    = then(LiteralArgumentBuilder.literal(name))