@file:Suppress("unused", "UNCHECKED_CAST")

package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.LOGGER
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.DetectedVersion
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.blocks.BlockInput
import net.silkmc.silk.commands.ArgumentCommandBuilder
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.text.literal

object VeinminerCommand {

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
            applySetting("searchRadius", { settings.searchRadius }) { settings.searchRadius = it }
            applySetting("permissionRestricted", { settings.permissionRestricted }) { settings.permissionRestricted = it }
        }

        literal("groups") {
            requires { Permissions.require(permissionGroups, 3).test(it) }
            fun groupExists(group: String): Boolean = ConfigManager.groups.any { it.name.lowercase() == group.lowercase() }
            fun getGroup(group: String): BlockGroup<String>? = if (!groupExists(group)) null else ConfigManager.groups.first { it.name.lowercase() ==  group.lowercase()}

            literal("list") {
                runs {
                    ConfigManager.groups.forEach { group ->
                        source.msg("group: ${group.name}", 0x0f0f0f)
                        source.msg("Blocks: ${group.blocks.joinToString(", ")}", 0x0f0f0f)
                        source.msg("", 0x000000)
                    }
                }
            }

            literal("add") {
                argument<String>("name") { name ->
                    runs {
                        val string = name().lowercase()
                        if (groupExists(string)) {
                            source.msg("$string already exists", cRed)
                            return@runs
                        } else {
                            ConfigManager.groups.add(BlockGroup<String>(string, mutableSetOf()))
                            source.msg("Created group $string", cGreen)
                        }
                    }
                }
            }

            literal("remove") {
                argument<String>("group") { group ->
                    suggestList { ConfigManager.groups.map { it.name }}
                    runs {
                        val name = group().lowercase()
                        if (!groupExists(name)) {
                            source.msg("$name is not a group", cRed)
                            return@runs
                        } else {
                            ConfigManager.groups.removeIf { it.name.lowercase() == name }
                            source.msg("group $name removed", cGreen)
                        }
                    }
                }
            }

            literal("edit") {
                argument<String>("group") { groupName ->
                    suggestList { ConfigManager.groups.map { it.name }}
                    literal("add") {
                        argument<BlockInput>("block") { block ->
                            runs {
                                val group = getGroup(groupName()) ?: run { source.msg("${groupName()} does not exist!", cRed); return@runs }
                                val blockId = block().state.block.descriptionId
                                if (group.blocks.contains(blockId)) {
                                    source.msg("$blockId is already present", cRed)
                                    return@runs
                                } else {
                                    group.blocks.add(blockId)
                                    source.msg("Added $blockId", cGreen)
                                }
                            }
                        }
                    }

                    literal("remove") {
                        fun <T> ArgumentCommandBuilder<CommandSourceStack, T>.suggestGroupBlocks(groupArgument: String) {
                            suggestList { info ->
                                val group = info.getArgument(groupArgument, String::class.java)
                                (getGroup(group)?: return@suggestList null).blocks
                            }
                        }

                        argument<BlockInput>("block") { block ->
                            suggestGroupBlocks("group")
                            runs {
                                val blockId = block().state.block.descriptionId
                                val group = getGroup(groupName()) ?: run { source.msg("${groupName()} does not exist!", cRed); return@runs }
                                if (!group.blocks.contains(blockId)) {
                                    source.msg("${group.name} does not contain $blockId", cRed)
                                    return@runs
                                } else {
                                    group.blocks.remove(blockId)
                                    source.msg("removed $blockId", cGreen)
                                }
                            }
                        }
                    }
                }
            }
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