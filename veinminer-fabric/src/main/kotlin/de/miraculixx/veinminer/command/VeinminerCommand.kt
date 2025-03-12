@file:Suppress("unused", "UNCHECKED_CAST")

package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.Veinminer.Companion.LOGGER
import de.miraculixx.veinminer.config.*
import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.utils.cBase
import de.miraculixx.veinminer.config.utils.cGreen
import de.miraculixx.veinminer.config.utils.cRed
import de.miraculixx.veinminer.config.utils.permissionBlocks
import de.miraculixx.veinminer.config.utils.permissionGroups
import de.miraculixx.veinminer.config.utils.permissionReload
import de.miraculixx.veinminer.config.utils.permissionSettings
import de.miraculixx.veinminer.config.utils.permissionToggle
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.DetectedVersion
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.commands.arguments.item.ItemInput
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

        literal("reload") {
            requires { Permissions.require(permissionReload, 3).test(it) }
            runs {
                ConfigManager.reload()
                source.msg("Veinminer config reloaded!", cGreen)
            }
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
            applySetting("mustSneak", { ConfigManager.settings.mustSneak }) { ConfigManager.settings.mustSneak = it }
            applySetting("cooldown", { ConfigManager.settings.cooldown }) { ConfigManager.settings.cooldown = it }
            applySetting("delay", { ConfigManager.settings.delay }) { ConfigManager.settings.delay = it }
            applySetting("maxChain", { ConfigManager.settings.maxChain }) { ConfigManager.settings.maxChain = it }
            applySetting("needCorrectTool", { ConfigManager.settings.needCorrectTool }) { ConfigManager.settings.needCorrectTool = it }
            applySetting("searchRadius", { ConfigManager.settings.searchRadius }) { ConfigManager.settings.searchRadius = it }
            applySetting("permissionRestricted", { ConfigManager.settings.permissionRestricted }) { ConfigManager.settings.permissionRestricted = it }
            applySetting("mergeItemDrops", { ConfigManager.settings.mergeItemDrops }) { ConfigManager.settings.mergeItemDrops = it }
            applySetting("decreaseDurability", { ConfigManager.settings.decreaseDurability }) { ConfigManager.settings.decreaseDurability = it }
        }


        literal("groups") {
            requires { Permissions.require(permissionGroups, 3).test(it) }
            fun groupExists(group: String) = ConfigManager.groups.firstOrNull { it.name.lowercase() == group.lowercase() }
            fun BlockInput.id() = state.block.descriptionId
            fun CommandSourceStack.createGroup(name: String, content: MutableSet<String>) {
                if (groupExists(name) != null) {
                    msg("Group '$name' already exists", cRed)
                    return
                }

                ConfigManager.groups.add(BlockGroup(name, content))
                msg("Created group '$name'\nAdd blocks with '/veinminer groups edit $name add ...'", cGreen)
                ConfigManager.save()
            }

            fun CommandSourceStack.editContent(groupName: String, material: String, isBlock: Boolean, isAdd: Boolean) {
                val group = groupExists(groupName) ?: return msg("Group '$groupName' does not exist", cRed)
                val set = if (isBlock) group.blocks else group.tools

                if (isAdd) {
                    if (set.add(material)) msg("Added $material to group '$groupName'", cGreen)
                    else return msg("$material is already in group '$groupName'", cRed)
                } else {
                    if (set.remove(material)) msg("Removed $material from group '$groupName'", cGreen)
                    else return msg("$material is not in group '$groupName'", cRed)
                }
                ConfigManager.save()
            }

            // Command description
            runs {
                source.msg(
                    "Groups can chain together multiple block types.\n" +
                            "E.g. creating a group 'oak' with oak_log & oak_leaves will veinmine the whole tree when breaking on part of it." +
                            "Groups can be limited to certain tools.", cBase
                )
            }

            // Display all present groups
            literal("list") {
                fun BlockGroup<String>.print(source: CommandSourceStack) {
                    source.msg("Group '${name}'", cBase)
                    source.msg(" -> Blocks: [${blocks.joinToString(", ")}]\n", cBase)
                    if (tools.isEmpty()) source.msg(" -> Tools: [all]\n", cBase)
                    else source.msg(" -> Tools: [${tools.joinToString(", ")}]\n", cBase)
                }

                argument<String>("group") { groupName ->
                    suggestList { ConfigManager.groups.map { it.name } }
                    runs {
                        val group = groupExists(groupName()) ?: return@runs source.msg("Group '$groupName' does not exist", cRed)
                        group.print(source)
                    }
                }

                runs {
                    ConfigManager.groups.forEach { group -> group.print(source) }
                }
            }

            // Create a new group with optional content
            literal("create") {
                argument<String>("name") { name ->
                    runs {
                        source.createGroup(name(), mutableSetOf())
                    }
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
                        if (groupExists(name) == null) {
                            source.msg("The group '$name' does not exist", cRed)
                            return@runs
                        }
                        ConfigManager.groups.removeIf { it.name.lowercase() == name }
                        source.msg("Removed group '$name'", cGreen)
                        ConfigManager.save()
                    }
                }
            }

            // Add or remove blocks from a group
            literal("edit") {
                argument<String>("group") { name ->
                    suggestList { ConfigManager.groups.map { it.name } }
                    literal("add-block") {
                        argument<BlockInput>("block") { block ->
                            runs {
                                source.editContent(name(), block().id(), true, true)
                            }
                        }
                    }

                    literal("remove-block") {
                        argument<String>("block") { block ->
                            suggestList { info ->
                                val group = info.getArgument("group", String::class.java)
                                groupExists(group)?.blocks
                            }
                            runs {
                                source.editContent(name(), block(), true, false)
                            }
                        }
                    }

                    literal("add-tool") {
                        argument<ItemInput>("tool") { tool ->
                            runs {
                                source.editContent(name(), tool().item.descriptionId, false, true)
                            }
                        }
                    }

                    literal("remove-tool") {
                        argument<String>("tool") { tool ->
                            suggestList { info ->
                                val group = info.getArgument("group", String::class.java)
                                groupExists(group)?.tools
                            }
                            runs {
                                source.editContent(name(), tool(), false, false)
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