@file:Suppress("DEPRECATION", "unused", "UNCHECKED_CAST")

package de.miraculixx.veinminer.command

import de.miraculixx.kpaper.chat.KColors
import de.miraculixx.kpaper.extensions.bukkit.addUrl
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.veinminer.INSTANCE
import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.extensions.color
import de.miraculixx.veinminer.config.utils.*
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.block.data.BlockData
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

object VeinminerCommand {
    private val command = commandTree("veinminer") {
        anyExecutor { sender, _ ->
            sender.sendMessage(
                cmp(
                    "Veinminer Version: ${INSTANCE.description.version} (paper)\n" +
                            "Game Version: ${INSTANCE.server.version}\n" +
                            "Download: "
                ) + cmp("modrinth.com/project/veinminer").addUrl("https://modrinth.com/project/veinminer")
            )
        }

        literalArgument("reload") {
            withPermission(permissionReload)
            anyExecutor { sender, _ ->
                ConfigManager.reload()
                sender.sendMessage(cmp("Veinminer config reloaded!", cGreen.color()))
            }
        }

        literalArgument("blocks") {
            withPermission(permissionBlocks)
            literalArgument("add") {
                blockStateArgument("block") {
                    anyExecutor { sender, args ->
                        val block = args[0] as BlockData
                        val name = block.material.key.asString()
                        if (ConfigManager.veinBlocks.add(block.material.key)) {
                            sender.sendMessage(cmp("Added $name to veinminer blocks", cGreen.color()))
                            ConfigManager.save()
                        } else {
                            sender.sendMessage(cmp("$name is already a veinminer block", cRed.color()))
                        }
                    }
                }
            }

            literalArgument("remove") {
                blockStateArgument("block") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.veinBlocks.map { it.asString() } })
                    anyExecutor { sender, args ->
                        val block = args[0] as BlockData
                        val material = block.material.key
                        if (ConfigManager.veinBlocks.remove(material)) {
                            sender.sendMessage(cmp("Removed ${material.asString()} from veinminer blocks", cGreen.color()))
                            ConfigManager.save()
                        } else {
                            sender.sendMessage(cmp("${material.asString()} is not a veinminer block", cRed.color()))
                        }
                    }
                }
            }
        }

        literalArgument("toggle") {
            withPermission(permissionToggle)
            anyExecutor { sender, _ ->
                if (VeinMinerEvent.enabled) {
                    VeinMinerEvent.enabled = false
                    sender.sendMessage(cmp("Veinminer functions disabled", cRed.color()))
                    return@anyExecutor
                }
                sender.sendMessage(cmp("Veinminer functions enabled", cGreen.color()))
                VeinMinerEvent.enabled = true
            }
        }

        literalArgument("settings") {
            withPermission(permissionSettings)
            applySetting("mustSneak", { ConfigManager.settings.mustSneak }) { ConfigManager.settings.mustSneak = it }
            applySetting("cooldown", { ConfigManager.settings.cooldown }) { ConfigManager.settings.cooldown = it }
            applySetting("delay", { ConfigManager.settings.delay }) { ConfigManager.settings.delay = it }
            applySetting("maxChain", { ConfigManager.settings.maxChain }) { ConfigManager.settings.maxChain = it }
            applySetting("needCorrectTool", { ConfigManager.settings.needCorrectTool }) { ConfigManager.settings.needCorrectTool = it }
            applySetting("searchRadius", { ConfigManager.settings.searchRadius }) { ConfigManager.settings.searchRadius = it }
            applySetting("permissionRestricted", { ConfigManager.settings.permissionRestricted }) { ConfigManager.settings.permissionRestricted = it }
            applySetting("mergeItemDrops", { ConfigManager.settings.mergeItemDrops }) { ConfigManager.settings.mergeItemDrops = it }
            applySetting("decreaseDurability", { ConfigManager.settings.decreaseDurability }) { ConfigManager.settings.decreaseDurability = it }
            applySetting("debug", { debug }) { debug = it }
            literalArgument("client") {
                applySetting("allow", { ConfigManager.settings.client.allow }) { ConfigManager.settings.client.allow = it }
                applySetting("translucentBlockHighlight", { ConfigManager.settings.client.translucentBlockHighlight }) { ConfigManager.settings.client.translucentBlockHighlight = it }
                applySetting("allowAllBlocks", { ConfigManager.settings.client.allBlocks }) { ConfigManager.settings.client.allBlocks = it }
            }
        }

        literalArgument("groups") {
            withPermission(permissionGroups)
            fun groupExists(group: String) = ConfigManager.groups.firstOrNull { it.name.lowercase() == group.lowercase() }
            fun CommandSender.createGroup(name: String, content: MutableSet<NamespacedKey>) {
                if (groupExists(name) != null) {
                    sendMessage(cmp("Group '$name' already exists", cRed.color()))
                    return
                }
                ConfigManager.groups.add(BlockGroup(name, content))
                ConfigManager.save()
                sendMessage(cmp("Created group '$name'\nAdd blocks with '/veinminer groups edit $name add ...'", cGreen.color()))
            }

            fun CommandSender.editContent(args: CommandArguments, material: NamespacedKey, isBlock: Boolean, isAdd: Boolean) {
                val groupName = args[0] as String
                val name = material.asString()
                val group = groupExists(groupName) ?: return sendMessage(cmp("Group '$groupName' does not exist", cRed.color()))
                val set = if (isBlock) group.blocks else group.tools

                if (isAdd) {
                    if (set.add(material)) sendMessage(cmp("Added $name to group '$groupName'", cGreen.color()))
                    else return sendMessage(cmp("$name is already in group '$groupName'", cRed.color()))
                } else {
                    if (set.remove(material)) sendMessage(cmp("Removed $name from group '$groupName'", cGreen.color()))
                    else return sendMessage(cmp("$name is not in group '$groupName'", cRed.color()))
                }
                ConfigManager.save()
            }

            literalArgument("list") {
                fun BlockGroup<NamespacedKey>.print(sender: CommandSender) {
                    sender.sendMessage(cmp("Group ") + cmp(name, NamedTextColor.WHITE))
                    sender.sendMessage(cmp(" -> Blocks: [") + cmp(blocks.joinToString(", ") { it.asString() }, NamedTextColor.WHITE) + cmp("]"))
                    if (tools.isEmpty()) sender.sendMessage(cmp(" -> Tools: [All]", NamedTextColor.WHITE))
                    else sender.sendMessage(cmp(" -> Tools: [") + cmp(tools.joinToString(", ") { it.asString() }, NamedTextColor.WHITE) + cmp("]"))

                }

                stringArgument("group") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.groups.map { it.name } })
                    anyExecutor { sender, args ->
                        val groupName = args[0] as String
                        val group = groupExists(groupName) ?: return@anyExecutor sender.sendMessage(cmp("Group '$groupName' does not exist", cRed.color()))
                        group.print(sender)
                    }
                }

                anyExecutor { sender, _ ->
                    ConfigManager.groups.forEach { group -> group.print(sender) }
                }
            }

            literalArgument("create") {
                stringArgument("name") {
                    anyExecutor { sender, args -> sender.createGroup(args[0] as String, mutableSetOf()) }
                    blockStateArgument("block1") {
                        anyExecutor { sender, args ->
                            sender.createGroup(args[0] as String, mutableSetOf((args[1] as BlockData).material.key))
                        }
                        blockStateArgument("block2") {
                            anyExecutor { sender, args ->
                                sender.createGroup(args[0] as String, mutableSetOf((args[1] as BlockData).material.key, (args[2] as BlockData).material.key))
                            }
                        }
                    }
                }
            }

            literalArgument("remove") {
                stringArgument("group") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.groups.map { it.name } })
                    anyExecutor { sender, args ->
                        val groupName = args[0] as String
                        val group = groupExists(groupName) ?: return@anyExecutor sender.sendMessage(cmp("Group '$groupName' does not exist", cRed.color()))

                        ConfigManager.groups.remove(group)
                        ConfigManager.save()
                        sender.sendMessage(cmp("Removed group '$name'", cGreen.color()))
                    }
                }
            }

            literalArgument("edit") {
                stringArgument("group") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.groups.map { it.name } })
                    literalArgument("add-block") {
                        blockStateArgument("block") {
                            anyExecutor { sender, args ->
                                sender.editContent(args, (args[1] as BlockData).material.key, true, true)
                            }
                        }
                    }

                    literalArgument("remove-block") {
                        blockStateArgument("block") {
                            replaceSuggestions(ArgumentSuggestions.stringCollection {
                                val group = it.previousArgs[0] as String
                                ConfigManager.groups.firstOrNull { g -> g.name == group }?.blocks?.map { b -> b.asString() } ?: emptyList()
                            })
                            anyExecutor { sender, args ->
                                sender.editContent(args, (args[1] as BlockData).material.key, true, false)
                            }
                        }
                    }

                    literalArgument("add-tool") {
                        itemStackArgument("tool") {
                            anyExecutor { sender, args ->
                                sender.editContent(args, (args[1] as ItemStack).type.key, false, true)
                            }
                        }
                    }

                    literalArgument("remove-tool") {
                        itemStackArgument("tool") {
                            replaceSuggestions(ArgumentSuggestions.stringCollection {
                                val group = it.previousArgs[0] as String
                                ConfigManager.groups.firstOrNull { g -> g.name == group }?.tools?.map { b -> b.asString() } ?: emptyList()
                            })
                            anyExecutor { sender, args ->
                                sender.editContent(args, (args[1] as ItemStack).type.key, false, false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> Argument<*>.applySetting(name: String, currentConsumer: () -> T, consumer: (T) -> Unit) {
        literalArgument(name) {
            anyExecutor { sender, _ ->
                sender.sendMessage(cmp(name, KColors.BLUE) + cmp(" is currently set to ") + cmp(currentConsumer.invoke().toString(), KColors.BLUE))
            }

            when (currentConsumer.invoke()) {
                is Boolean -> booleanArgument("new") {
                    applyValue(name, consumer)
                }

                is Int -> integerArgument("new", min = 0) {
                    applyValue(name, consumer)
                }
            }
        }
    }

    private fun <T> Argument<*>.applyValue(name: String, consumer: (T) -> Unit) {
        anyExecutor { sender, args ->
            val new = args[0] as T
            consumer.invoke(new)
            sender.sendMessage(cmp("$name set to $new", cGreen.color()))
            ConfigManager.save()
        }
    }
}