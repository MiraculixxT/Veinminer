@file:Suppress("unused", "UNCHECKED_CAST", "BooleanLiteralArgument")

package de.miraculixx.veinminer.command

import com.mojang.brigadier.context.CommandContext
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.Veinminer.Companion.LOGGER
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.utils.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.DetectedVersion
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument
import net.minecraft.commands.arguments.item.ItemPredicateArgument
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.text.literal

object VeinminerCommand {

    private val command = command("veinminer") {
        runsAsync {
            source.msg(
                "Veinminer Version: ${Veinminer.INSTANCE.metadata.version} (fabric)\n" +
                        "Game Version: ${DetectedVersion.tryDetectVersion().name()}", cBase
            )
        }

        literal("reload") {
            requires { Permissions.require(permissionReload, 3).test(it) }
            runsAsync {
                ConfigManager.reload(true)
                source.msg("Veinminer config reloaded!", cGreen)
            }
        }

        literal("blocks") { // 1
            requires { Permissions.require(permissionBlocks, 3).test(it) }
            literal("add") { // 2
                argument<BlockPredicateArgument.Result>("block", BlockPredicateArgument::blockPredicate) { block -> // 3
                    runsAsync {
                        val id = getRaw(3) ?: return@runsAsync source.msg("Block can not be null", cRed)
                        if (ConfigManager.veinBlocksRaw.add(id)) {
                            ConfigManager.save()
                            source.msg("Added $id to veinminer blocks", cGreen)
                        } else {
                            source.msg("$id is already a veinminer block", cRed)
                        }
                    }
                }
            }

            literal("remove") {
                argument<BlockPredicateArgument.Result>("block", BlockPredicateArgument::blockPredicate) { block -> // 3
                    suggestListSuspending { ctx -> ctx.suggestFilteredList(ConfigManager.veinBlocksRaw) }
                    runsAsync {
                        val id = getRaw(3) ?: return@runsAsync source.msg("Block can not be null", cRed)
                        if (ConfigManager.veinBlocksRaw.remove(id)) {
                            ConfigManager.save()
                            source.msg("Removed $id from veinminer blocks", cGreen)
                        } else {
                            source.msg("$id is not a veinminer block", cRed)
                        }
                    }
                }
            }
        }

        literal("toggle") {
            requires { Permissions.require(permissionToggle, 3).test(it) }
            runsAsync {
                if (Veinminer.active) source.msg("Veinminer functions disabled", cRed)
                else source.msg("Veinminer functions enabled", cGreen)
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
            applySetting("preventToolBreaking", { ConfigManager.settings.preventToolBreaking }) { ConfigManager.settings.preventToolBreaking = it }
            applySetting("debug", { debug }) { debug = it }
            literal("client") {
                applySetting("allow", { ConfigManager.settings.client.allow }) { ConfigManager.settings.client.allow = it }
                applySetting("translucentBlockHighlight", { ConfigManager.settings.client.translucentBlockHighlight }) { ConfigManager.settings.client.translucentBlockHighlight = it }
                applySetting("allowAllBlocks", { ConfigManager.settings.client.allBlocks }) { ConfigManager.settings.client.allBlocks = it }
            }
        }


        literal("groups") { // 1
            requires { Permissions.require(permissionGroups, 3).test(it) }
            fun groupExists(group: String) = ConfigManager.groupsRaw.firstOrNull { it.name.lowercase() == group.lowercase() }
            fun BlockInput.id() = state.block.descriptionId
            fun CommandSourceStack.createGroup(name: String, content: MutableSet<String>) {
                if (groupExists(name) != null) {
                    msg("Group '$name' already exists", cRed)
                    return
                }

                ConfigManager.groupsRaw.add(BlockGroup(name, content))
                msg("Created group '$name'\nAdd blocks with '/veinminer groups edit $name add ...'", cGreen)
                ConfigManager.save()
            }

            fun CommandSourceStack.editContent(groupName: String, rawKey: String?, isBlock: Boolean, isAdd: Boolean) {
                val group = groupExists(groupName) ?: return msg("Group '$groupName' does not exist", cRed)
                val set = if (isBlock) group.blocks else group.tools

                if (rawKey == null) return msg("Invalid material", cRed)
                if (isAdd) {
                    if (set.add(rawKey)) msg("Added $rawKey to group '$groupName'", cGreen)
                    else return msg("$rawKey is already in group '$groupName'", cRed)
                } else {
                    if (set.remove(rawKey)) msg("Removed $rawKey from group '$groupName'", cGreen)
                    else return msg("$rawKey is not in group '$groupName'", cRed)
                }
                ConfigManager.save()
            }

            // Command description
            runsAsync {
                source.msg(
                    "Groups can chain together multiple block types.\n" +
                            "E.g. creating a group 'oak' with oak_log & oak_leaves will veinmine the whole tree when breaking on part of it." +
                            "Groups can be limited to certain tools.", cBase
                )
            }

            // Display all present groups
            literal("list") {
                // 2
                fun BlockGroup<String>.print(source: CommandSourceStack) {
                    source.msg("Group '${name}'", cBase)
                    source.msg(" -> Blocks: [${blocks.joinToString(", ")}]\n", cBase)
                    if (tools.isEmpty()) source.msg(" -> Tools: [all]\n", cBase)
                    else source.msg(" -> Tools: [${tools.joinToString(", ")}]\n", cBase)
                }

                argument<String>("group") { groupName ->
                    suggestListSuspending { ConfigManager.groupsRaw.map { it.name } }
                    runsAsync {
                        val group = groupExists(groupName()) ?: return@runsAsync source.msg("Group '$groupName' does not exist", cRed)
                        group.print(source)
                    }
                }

                runsAsync {
                    ConfigManager.groupsRaw.forEach { group -> group.print(source) }
                }
            }

            // Create a new group with optional content
            literal("create") { // 2
                argument<String>("name") { name -> // 3
                    runsAsync {
                        source.createGroup(name(), mutableSetOf())
                    }
                    argument<BlockPredicateArgument.Result>("block1", BlockPredicateArgument::blockPredicate) { block1 -> // 4
                        runsAsync { source.createGroup(name(), getRaw(4)?.let { mutableSetOf(it) } ?: mutableSetOf()) }
                        argument<BlockPredicateArgument.Result>("block2", BlockPredicateArgument::blockPredicate) { block2 -> // 5
                            runsAsync { source.createGroup(name(), setOf(getRaw(5), getRaw(6)).mapNotNull { it }.toMutableSet()) }
                        }
                    }
                }
            }

            // Remove an existing group
            literal("remove") { // 2
                argument<String>("group") { group -> // 3
                    suggestListSuspending { ConfigManager.groupsRaw.map { it.name } }
                    runsAsync {
                        val name = group().lowercase()
                        val group = groupExists(name) ?: return@runsAsync source.msg("The group '$name' does not exist", cRed)
                        ConfigManager.groupsRaw.remove(group)
                        source.msg("Removed group '$name'", cGreen)
                        ConfigManager.save()
                    }
                }
            }

            // Add or remove blocks from a group
            literal("edit") { // 2
                argument<String>("group") { name -> // 3
                    suggestListSuspending { ConfigManager.groupsRaw.map { it.name } }
                    literal("add-block") { // 4
                        argument<BlockPredicateArgument.Result>("block", BlockPredicateArgument::blockPredicate) { block -> // 5
                            runsAsync {
                                source.editContent(name(), getRaw(5), true, true)
                            }
                        }
                    }

                    literal("remove-block") { // 4
                        argument<BlockPredicateArgument.Result>("block", BlockPredicateArgument::blockPredicate) { block -> // 5
                            suggestListSuspending { ctx ->
                                val group = ctx.getArgument("group", String::class.java)
                                ctx.suggestFilteredList(groupExists(group)?.blocks)
                            }
                            runsAsync {
                                source.editContent(name(), getRaw(5), true, false)
                            }
                        }
                    }

                    literal("add-tool") { // 4
                        argument<ItemPredicateArgument.Result>("tool", ItemPredicateArgument::itemPredicate) { tool -> // 5
                            runsAsync {
                                source.editContent(name(), getRaw(5), false, true)
                            }
                        }
                    }

                    literal("remove-tool") { // 4
                        argument<ItemPredicateArgument.Result>("tool", ItemPredicateArgument::itemPredicate) { tool -> // 5
                            suggestListSuspending { ctx ->
                                val group = ctx.getArgument("group", String::class.java)
                                ctx.suggestFilteredList(groupExists(group)?.tools)
                            }
                            runsAsync {
                                source.editContent(name(), getRaw(5), false, false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> LiteralCommandBuilder<CommandSourceStack>.applySetting(name: String, currentConsumer: () -> T, consumer: (T) -> Unit) {
        literal(name) {
            runsAsync {
                source.msg("$name is currently set to ${currentConsumer.invoke()}", cBase)
            }

            when (currentConsumer.invoke()) {
                is Boolean -> argument<Boolean>("new") { new ->
                    runsAsync {
                        val value = new() as T
                        consumer.invoke(value)
                        ConfigManager.save()
                        source.msg("$name set to $value", cGreen)
                    }
                }

                is Int -> argument<Int>("new") { new ->
                    runsAsync {
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

    private fun CommandContext<CommandSourceStack>.suggestFilteredList(list: Collection<String>?): List<String> {
        val input = input.split(' ').lastOrNull() ?: ""
        return list?.filter { input.isEmpty() || it.startsWith(input) } ?: emptyList()
    }

    private fun CommandContext<CommandSourceStack>.getRaw(idx: Int) = input.split(' ').getOrNull(idx)
}