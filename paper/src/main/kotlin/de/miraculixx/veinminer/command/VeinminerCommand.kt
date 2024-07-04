@file:Suppress("DEPRECATION", "unused", "UNCHECKED_CAST")

package de.miraculixx.veinminer.command

import de.miraculixx.kpaper.extensions.bukkit.addUrl
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.veinminer.INSTANCE
import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.config.*
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.data.BlockData

object VeinminerCommand {
    private val command = commandTree("veinminer") {
        anyExecutor { sender, _ ->
            sender.sendMessage(cmp("Veinminer Version: ${INSTANCE.description.version} (paper)\n" +
                        "Game Version: ${INSTANCE.server.version}" +
                        "Download: "
            ) + cmp("modrinth.com/project/veinminer").addUrl("https://modrinth.com/project/veinminer"))
        }

        literalArgument("blocks") {
            withPermission(permissionBlocks)
            literalArgument("add") {
                blockStateArgument("block") {
                    anyExecutor { sender, args ->
                        val block = args[0] as BlockData
                        val name = block.material.name.fancy()
                        if (ConfigManager.veinBlocks.add(block.material)) {
                            sender.sendMessage(cmp("Added $name to veinminer blocks", cGreen.color()))
                            ConfigManager.save()
                        } else {
                            sender.sendMessage(cmp("$name is already a veinminer block", cRed.color()))
                        }
                    }
                }
            }

            literalArgument("remove") {
                stringArgument("block") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.veinBlocks.map { it.name.lowercase() } })
                    anyExecutor { sender, args ->
                        val string = args[0] as String
                        val material = enumOf<Material>(string.uppercase())
                        if (material == null) {
                            sender.sendMessage(cmp("$string is not a valid material", cRed.color()))
                            return@anyExecutor
                        }
                        val name = string.fancy()
                        if (ConfigManager.veinBlocks.remove(material)) {
                            sender.sendMessage(cmp("Removed $name from veinminer blocks", cGreen.color()))
                            ConfigManager.save()
                        } else {
                            sender.sendMessage(cmp("$name is not a veinminer block", cRed.color()))
                        }
                    }
                }
            }
        }

        literalArgument("toggle") {
            withPermission(permissionToggle)
            anyExecutor { sender, _ ->
                if (Veinminer.eventInstance == null) {
                    sender.sendMessage(cmp("Veinminer functions enabled", cGreen.color()))
                    Veinminer.eventInstance = VeinMinerEvent()
                    return@anyExecutor
                }
                sender.sendMessage(cmp("Veinminer functions disabled", cRed.color()))
                Veinminer.eventInstance?.disable()
                Veinminer.eventInstance = null
            }
        }

        literalArgument("settings") {
            withPermission(permissionSettings)
            val settings = ConfigManager.settings
            applySetting("mustSneak", { settings.mustSneak }) { settings.mustSneak = it }
            applySetting("cooldown", { settings.cooldown }) { settings.cooldown = it }
            applySetting("delay", { settings.delay }) { settings.delay = it }
            applySetting("maxChain", { settings.maxChain }) { settings.maxChain = it }
            applySetting("needCorrectTool", { settings.needCorrectTool }) { settings.needCorrectTool = it }
            applySetting("searchRadius", { settings.searchRadius }) { settings.searchRadius = it }
            applySetting("permissionRestricted", { settings.permissionRestricted }) { settings.permissionRestricted = it }
        }

        literalArgument("groups") {
            withPermission(permissionGroups)
            literalArgument("list") {

                anyExecutor { sender, _ ->
                    ConfigManager.groups.forEach { group ->
                        sender.sendMessage(cmp("group: ") + cmp(group.name, NamedTextColor.WHITE))
                        sender.sendMessage(cmp("Blocks: ") + cmp(group.blocks.map { it.name.fancy() }.joinToString(", "), NamedTextColor.WHITE))
                        sender.sendMessage("")
                    }
                }

                stringArgument("group") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.groups.map { it.name } })

                    anyExecutor { sender, args ->
                        val group: String by args
                        if (!ConfigManager.groups.any { it.name.lowercase() == group.lowercase() }) {
                            sender.sendMessage(cmp("$group not found!", cRed.color()))
                            return@anyExecutor
                        }
                        val groupO = ConfigManager.groups.first { it.name.lowercase() == group.lowercase() }
                        sender.sendMessage(cmp("Group:") + cmp(" ${groupO.name}", NamedTextColor.WHITE))
                        sender.sendMessage(cmp("Blocks:") + cmp(groupO.blocks.map { it.name.fancy() }.joinToString(", "), NamedTextColor.WHITE))
                    }

                }

            }

            literalArgument("add") {

                stringArgument("name") {

                    blockStateArgument("block") {

                        anyExecutor { sender, args ->
                            val name: String by args
                            val block: BlockData by args
                            if (ConfigManager.groups.any { it.name.lowercase() == name.lowercase() }) {
                                sender.sendMessage(cmp("$name already exists!", cRed.color()))
                                return@anyExecutor
                            }
                            ConfigManager.groups.add(BlockGroup(name, mutableSetOf(block.material)))
                            sender.sendMessage(cmp("Created group $name", cGreen.color()))
                        }

                    }

                }

            }

            literalArgument("remove") {

                stringArgument("group") {

                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.groups.map { it.name } })

                    anyExecutor { sender, args ->
                        val group: String by args
                        if (!ConfigManager.groups.any { it.name.lowercase() == group.lowercase() }) {
                            sender.sendMessage(cmp("Group $group not found!", cRed.color()))
                            return@anyExecutor
                        }
                        ConfigManager.groups.removeIf { it.name.lowercase() == group.lowercase()}
                        sender.sendMessage(cmp("Group removed!", cGreen.color()))
                    }
                }

            }

            literalArgument("edit") {

                stringArgument("group") {

                    replaceSuggestions(ArgumentSuggestions.stringCollection { ConfigManager.groups.map { it.name } })

                    literalArgument("add") {

                        blockStateArgument("block") {

                            anyExecutor { sender, args ->

                                val group: String by args
                                val block: BlockData by args
                                val name = block.material.name.fancy()
                                if (!ConfigManager.groups.any { it.name.lowercase() == group.lowercase() }) {
                                    sender.sendMessage(cmp("$group not found!", cRed.color()))
                                    return@anyExecutor
                                }
                                if (!ConfigManager.groups.first { it.name.lowercase() == group.lowercase() }.blocks.add(block.material)) {
                                    sender.sendMessage(cmp("$name is already present in this group!", cRed.color()))
                                    return@anyExecutor
                                } else {
                                    sender.sendMessage(cmp("$name added to $group!", cGreen.color()))
                                }

                            }

                        }

                    }

                    literalArgument("remove") {
                        blockStateArgument("block") {
                            anyExecutor { sender, args ->
                                val group: String by args
                                val block: BlockData by args
                                val name = block.material.name.fancy()
                                if (!ConfigManager.groups.any { it.name.lowercase() == group.lowercase() }) {
                                    sender.sendMessage(cmp("$group not found!", cRed.color()))
                                    return@anyExecutor
                                }

                                if (!ConfigManager.groups.first { it.name.lowercase() == group.lowercase() }.blocks.remove(block.material)) {
                                    sender.sendMessage(cmp("$name is not present in this group!", cRed.color()))
                                    return@anyExecutor
                                } else {
                                    sender.sendMessage(cmp("$name removed from $group!", cGreen.color()))
                                }
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
                sender.sendMessage(cmp("$name is currently set to ${currentConsumer.invoke()}"))
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