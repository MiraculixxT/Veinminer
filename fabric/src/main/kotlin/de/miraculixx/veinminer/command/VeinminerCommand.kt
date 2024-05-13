package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.LOGGER
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.DetectedVersion
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.blocks.BlockInput
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.text.literal

object VeinminerCommand {

//    init {
//        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, registryAccess, environment ->
//            dispatcher.register(literal<CommandSourceStack>("veinminer")
//                .executes { context ->
//                    context.source.sendMessage(("Veinminer Version: ${Veinminer.INSTANCE.metadata.version} (fabric)\n" +
//                                "Game Version: ${DetectedVersion.tryDetectVersion().name}").literal)
//                    1
//                }.then(literal<CommandSourceStack>("blocks")
//                    .requires(Permissions.require(permissionBlocks, 3))
//                    .then(literal<CommandSourceStack>("add")
//                        .then(LiteralArgumentBuilder.argument("block", BlockInput.block())
//                            .executes { context ->
//                                val state = BlockInput.block(context, "block").state.block
//                                val name = state.name
//                                println(state.descriptionId)
//                                if (ConfigManager.veinBlocks.add(state.descriptionId)) {
//                                    context.source.sendMessage("Added $name to veinminer blocks".literal.withColor(cGreen))
//                                    ConfigManager.save()
//                                } else {
//                                    context.source.sendMessage("$name is already a veinminer block".literal.withColor(cRed))
//                                }
//                                1
//                            }
//                        )
//                    )
//                    .then(LiteralArgumentBuilder.literal<CommandSourceStack>("remove")
//                        .then(LiteralArgumentBuilder.argument("block", StringArgumentType.string())
//                            .suggests { ConfigManager.veinBlocks.toList() }
//                            .executes { context ->
//                                val string = StringArgumentType.getString(context, "block")
//                                val name = string.lowercase().replace("_", " ")
//                                if (ConfigManager.veinBlocks.remove(string)) {
//                                    context.source.sendMessage("Removed $name from veinminer blocks".literal.withColor(cGreen))
//                                    ConfigManager.save()
//                                } else {
//                                    context.source.sendMessage("$name is not a veinminer block".literal.withColor(cRed))
//                                }
//                                1
//                            }
//                        )
//                    )
//                )
//        })
//    }

    private val command = command("veinminer") {
        runs {
            source.msg(
                "Veinminer Version: ${Veinminer.INSTANCE.metadata.version} (fabric)\n" +
                        "Game Version: ${DetectedVersion.tryDetectVersion().name}", cBase
            )
        }

        literal("blocks") {
            requires { Permissions.require(permissionBlocks, 3).test(it) }
            literal("add") {
                argument<BlockInput>("block") { block ->
                    runs {
                        val id = block().state.block.descriptionId
                        if (ConfigManager.veinBlocks.add(id)) {
                            ConfigManager.save()
                            source.msg("Added $id to veinminer blocks", cGreen)
                        } else {
                            source.msg("$id is already a veinminer block", cRed)
                        }
                    }
                }
            }

            literal("remove") {
                argument<String>("block") { block ->
                    suggestList { ConfigManager.veinBlocks.toList() }
                    runs {
                        val string = block()
                        if (ConfigManager.veinBlocks.remove(string)) {
                            ConfigManager.save()
                            source.msg("Removed $string from veinminer blocks", cGreen)
                        } else {
                            source.msg("$string is not a veinminer block", cRed)
                        }
                    }
                }
            }
        }

        literal("toggle") {
            requires { Permissions.require(permissionToggle, 3).test(it) }
            runs {
                if (Veinminer.active) source.msg("Veinminer functions disabled", cGreen)
                else source.msg("Veinminer functions enabled", cRed)
                Veinminer.active = !Veinminer.active
            }
        }

        literal("settings") {
            requires { Permissions.require(permissionSettings, 3).test(it) }
            val settings = ConfigManager.settings
            applySetting("mustSneak", { settings.mustSneak }) { settings.mustSneak = it }
            applySetting("cooldown", { settings.cooldown }) { settings.cooldown = it }
            applySetting("delay", { settings.delay }) { settings.delay = it }
            applySetting("maxChain", { settings.maxChain }) { settings.maxChain = it }
            applySetting("needCorrectTool", { settings.needCorrectTool }) { settings.needCorrectTool = it }
        }
    }

    private fun <T> LiteralCommandBuilder<CommandSourceStack>.applySetting(name: String, currentConsumer: () -> T, consumer: (T) -> Unit) {
        literal(name) {
            runs {
                source.msg("$name is currently set to ${currentConsumer.invoke()}", cBase)
            }

            when (currentConsumer.invoke()) {
                is Boolean -> argument<Boolean>("new") { new ->
                    runs {
                        val value = new() as T
                        consumer.invoke(value)
                        ConfigManager.save()
                        source.msg("$name set to $value", cGreen)
                    }
                }

                is Int -> argument<Int>("new") { new ->
                    runs {
                        val value = new() as T
                        consumer.invoke(value)
                        ConfigManager.save()
                        source.msg("$name set to $value", cGreen)
                    }
                }
            }
        }
    }

    private fun CommandSourceStack.msg(message: String, color: Int) {
        try {
            sendSystemMessage(message.literal.withColor(color))
        } catch (_: Exception) {
            sendSystemMessage(message.literal)
        } catch (_: Exception) {
            LOGGER.info("Messages cannot be sent in this version")
        }
    }
}