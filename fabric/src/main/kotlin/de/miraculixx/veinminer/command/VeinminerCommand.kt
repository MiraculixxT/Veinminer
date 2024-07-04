@file:Suppress("unused", "UNCHECKED_CAST")

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
            fun groupExists(group: String) = ConfigManager.groups.any { it.name.lowercase() == group.lowercase() }
            fun getGroup(group: String): BlockGroup<String>? = if (!groupExists(group)) null else ConfigManager.groups.first { it.name.lowercase() == group.lowercase() }
            fun BlockInput.id() = state.block.descriptionId
            fun CommandSourceStack.createGroup(name: String, content: MutableSet<String>) {
                if (groupExists(name)) {
                    msg("Group '$name' already exists", cRed)
                    return
                }
                ConfigManager.groups.add(BlockGroup(name, content))
                msg("Created group '$name'\nAdd blocks with '/groups edit $name add ...'", cGreen)
            }

            // Command description
            runs {
                source.msg(
                    "Groups can chain together multiple block types.\n" +
                            "E.g. creating a group 'oak' with oak_log & oak_leaves will veinmine the whole tree when breaking on part of it.", cBase
                )
            }

            // Display all present groups
            literal("list") {
                runs {
                    ConfigManager.groups.forEach { group ->
                        source.msg("Group '${group.name}'", cBase)
                        source.msg(" -> Blocks: [${group.blocks.joinToString(", ")}]\n", cBase)
                    }
                }
            }

            // Create a new group with optional content
            literal("create") {
                argument<String>("name") { name ->
                    runs { source.createGroup(name(), mutableSetOf()) }
                    argument<BlockInput>("block1") { block1 ->
                        runs { source.createGroup(name(), mutableSetOf(block1().id())) }
                        argument<BlockInput>("block2") { block2 ->
                            runs { source.createGroup(name(), mutableSetOf(block1().id(), block2().id())) }
                        }
                    }
                }
            }

            // Remove an existing group
            literal("remove") {
                argument<String>("group") { group ->
                    suggestList { ConfigManager.groups.map { it.name } }
                    runs {
                        val name = group().lowercase()
                        if (!groupExists(name)) {
                            source.msg("The group '$name' does not exist", cRed)
                            return@runs
                        }
                        ConfigManager.groups.removeIf { it.name.lowercase() == name }
                        source.msg("Removed group '$name'", cGreen)
                    }
                }
            }

            // Add or remove blocks from a group
            literal("edit") {
                argument<String>("group") { name ->
                    suggestList { ConfigManager.groups.map { it.name } }
                    literal("add") {
                        argument<BlockInput>("block") { block ->
                            runs {
                                val group = getGroup(name()) ?: run { source.msg("Group '${name()}' does not exist!", cRed); return@runs }
                                val blockId = block().id()
                                if (group.blocks.contains(blockId)) {
                                    source.msg("Block '$blockId' is already present in group '${name()}'", cRed)
                                    return@runs
                                }
                                group.blocks.add(blockId)
                                source.msg("Added '$blockId' to the group '${name()}'", cGreen)
                            }
                        }
                    }

                    literal("remove") {
                        argument<BlockInput>("block") { block ->
                            suggestList { info ->
                                val group = info.getArgument("group", String::class.java)
                                (getGroup(group) ?: return@suggestList null).blocks
                            }
                            runs {
                                val blockId = block().id()
                                val group = getGroup(name()) ?: run { source.msg("Group '${name()}' does not exist!", cRed); return@runs }
                                if (!group.blocks.contains(blockId)) {
                                    source.msg("Blocks '$blockId' is not present in group '${group.name}'", cRed)
                                    return@runs
                                }
                                group.blocks.remove(blockId)
                                source.msg("Removed '$blockId' from the group '${name()}'", cGreen)
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