@file:Suppress("unused", "UNCHECKED_CAST", "BooleanLiteralArgument")

package de.miraculixx.veinminer.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import de.miraculixx.veinminer.ActiveConfig
import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.utils.cBase
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.debug
import de.miraculixx.veinminer.utils.permissionBlocks
import de.miraculixx.veinminer.utils.permissionGroups
import de.miraculixx.veinminer.utils.permissionReload
import de.miraculixx.veinminer.utils.permissionSettings
import de.miraculixx.veinminer.utils.permissionToggle
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument
import net.minecraft.commands.arguments.item.ItemPredicateArgument
import net.minecraft.network.chat.Component
import kotlin.reflect.typeOf

object VeinminerCommand {

    fun build(ctx: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> =
        veinminerCommand("veinminer") {
            executesAsync {
                val host = ActiveHost.host
                source.msg("Veinminer Version: ${host.versionVeinminer} (${host.platform})\n" +
                        "Minecraft Version: ${host.versionMinecraft}", cBase
                )
            }

            literal("reload") {
                requiresPermission(permissionReload)
                executesAsync {
                    ActiveConfig.bridge.reload(true)
                    source.msg("Veinminer config reloaded!", cGreen)
                }
            }

            literal("blocks") {
                requiresPermission(permissionBlocks)
                executesAsync { source.msg("Correct Syntax: /veinminer blocks <add/remove> <block>", cRed) }

                literal("add") {
                    argument("block", BlockPredicateArgument.blockPredicate(ctx)) {
                        executesAsync {
                            val id = getRaw(3) ?: return@executesAsync source.msg("Block can not be null", cRed)
                            if (ActiveConfig.bridge.veinBlocksRaw.add(id)) {
                                ActiveConfig.bridge.save()
                                source.msg("Added $id to veinminer blocks", cGreen)
                            } else {
                                source.msg("$id is already a veinminer block", cRed)
                            }
                        }
                    }
                }

                literal("remove") {
                    argument("block", BlockPredicateArgument.blockPredicate(ctx)) {
                        suggestStrings { c -> c.suggestFilteredList(ActiveConfig.bridge.veinBlocksRaw) }
                        executesAsync {
                            val id = getRaw(3) ?: return@executesAsync source.msg("Block can not be null", cRed)
                            if (ActiveConfig.bridge.veinBlocksRaw.remove(id)) {
                                ActiveConfig.bridge.save()
                                source.msg("Removed $id from veinminer blocks", cGreen)
                            } else {
                                source.msg("$id is not a veinminer block", cRed)
                            }
                        }
                    }
                }
            }

            literal("toggle") {
                requiresPermission(permissionToggle)
                executesAsync {
                    if (ActiveHost.host.active) source.msg("Veinminer functions disabled", cRed)
                    else source.msg("Veinminer functions enabled", cGreen)
                    ActiveHost.host.active = !ActiveHost.host.active
                }
            }

            literal("settings") {
                requiresPermission(permissionSettings)
                executesAsync { source.msg("Correct Syntax: /veinminer settings <setting> [<new-value>]", cRed) }

                applySetting("mustSneak", { ActiveConfig.bridge.settings.mustSneak }) { x, _ -> ActiveConfig.bridge.settings.mustSneak = x }
                applySetting("cooldown", { ActiveConfig.bridge.settings.cooldown }) { x, _ -> ActiveConfig.bridge.settings.cooldown = x }
                applySetting("delay", { ActiveConfig.bridge.settings.delay }) { x, _ -> ActiveConfig.bridge.settings.delay = x }
                applySetting("maxChain", { ActiveConfig.bridge.settings.maxChain }) { x, _ -> ActiveConfig.bridge.settings.maxChain = x }
                applySetting("needCorrectTool", { ActiveConfig.bridge.settings.needCorrectTool }) { x, _ -> ActiveConfig.bridge.settings.needCorrectTool = x }
                applySetting("searchRadius", { ActiveConfig.bridge.settings.searchRadius }) { x, _ -> ActiveConfig.bridge.settings.searchRadius = x }
                applySetting("permissionRestricted", { ActiveConfig.bridge.settings.permissionRestricted }) { x, _ -> ActiveConfig.bridge.settings.permissionRestricted = x }
                applySetting("mergeItemDrops", { ActiveConfig.bridge.settings.mergeItemDrops }) { x, _ -> ActiveConfig.bridge.settings.mergeItemDrops = x }
                applySetting("decreaseDurability", { ActiveConfig.bridge.settings.decreaseDurability }) { x, _ -> ActiveConfig.bridge.settings.decreaseDurability = x }
                applySetting("miningSpeedModifier", { ActiveConfig.bridge.settings.miningSpeedModifier }) { x, _ -> ActiveConfig.bridge.settings.miningSpeedModifier = x }
                applySetting("debug", { debug }) { x, _ -> ActiveConfig.bridge.settings.debug = x }
                literal("client") {
                    applySetting("allow", { ActiveConfig.bridge.settings.client.allow }) { x, _ -> ActiveConfig.bridge.settings.client.allow = x }
                    applySetting("require", { ActiveConfig.bridge.settings.client.require }) { x, _ -> ActiveConfig.bridge.settings.client.require = x }
                    applySetting("translucentBlockHighlight", { ActiveConfig.bridge.settings.client.translucentBlockHighlight }) { x, _ -> ActiveConfig.bridge.settings.client.translucentBlockHighlight = x }
                    applySetting("allowAllBlocks", { ActiveConfig.bridge.settings.client.allBlocks }) { x, _ -> ActiveConfig.bridge.settings.client.allBlocks = x }
                    literal("override") {
                        overrides { _ -> ActiveConfig.bridge.settings.client.overrides }
                    }
                }
            }

            literal("groups") {
                requiresPermission(permissionGroups)
                fun groupExists(group: String) =
                    ActiveConfig.bridge.groupsRaw.firstOrNull { it.name.equals(group, ignoreCase = true) }

                fun CommandSourceStack.createGroup(name: String, content: MutableSet<String>) {
                    if (groupExists(name) != null) {
                        msg("Group '$name' already exists", cRed)
                        return
                    }
                    ActiveConfig.bridge.groupsRaw.add(BlockGroup(name, content))
                    msg("Created group '$name'\nAdd blocks with '/veinminer groups edit $name add ...'", cGreen)
                    ActiveConfig.bridge.save()
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
                    ActiveConfig.bridge.save()
                }

                executesAsync {
                    source.msg(
                        "Groups can chain together multiple block types.\n" +
                                "E.g. creating a group 'oak' with oak_log & oak_leaves will veinmine the whole tree when breaking on part of it." +
                                "Groups can be limited to certain tools.", cBase
                    )
                }

                literal("list") {
                    fun BlockGroup<String>.print(source: CommandSourceStack) {
                        source.msg("Group '${name}'", cBase)
                        source.msg(" -> Blocks: [${blocks.joinToString(", ")}]\n", cBase)
                        if (tools.isEmpty()) source.msg(" -> Tools: [all]\n", cBase)
                        else source.msg(" -> Tools: [${tools.joinToString(", ")}]\n", cBase)
                    }

                    stringArg("group") {
                        suggestStrings { ActiveConfig.bridge.groupsRaw.map { it.name } }
                        executesAsync {
                            val name = StringArgumentType.getString(this, "group")
                            val group = groupExists(name) ?: return@executesAsync source.msg("Group '$name' does not exist", cRed)
                            group.print(source)
                        }
                    }

                    executesAsync {
                        ActiveConfig.bridge.groupsRaw.forEach { group -> group.print(source) }
                    }
                }

                literal("create") {
                    stringArg("name") {
                        executesAsync {
                            val name = StringArgumentType.getString(this, "name")
                            source.createGroup(name, mutableSetOf())
                        }
                        argument("block1", BlockPredicateArgument.blockPredicate(ctx)) {
                            executesAsync {
                                val name = StringArgumentType.getString(this, "name")
                                source.createGroup(name, getRaw(4)?.let { mutableSetOf(it) } ?: mutableSetOf())
                            }
                            argument("block2", BlockPredicateArgument.blockPredicate(ctx)) {
                                executesAsync {
                                    val name = StringArgumentType.getString(this, "name")
                                    source.createGroup(name, setOf(getRaw(4), getRaw(5)).mapNotNull { it }.toMutableSet())
                                }
                            }
                        }
                    }
                }

                literal("remove") {
                    stringArg("group") {
                        suggestStrings { ActiveConfig.bridge.groupsRaw.map { it.name } }
                        executesAsync {
                            val name = StringArgumentType.getString(this, "group").lowercase()
                            val group = groupExists(name) ?: return@executesAsync source.msg("The group '$name' does not exist", cRed)
                            ActiveConfig.bridge.groupsRaw.remove(group)
                            source.msg("Removed group '$name'", cGreen)
                            ActiveConfig.bridge.save()
                        }
                    }
                }

                literal("edit") {
                    stringArg("group") {
                        suggestStrings { ActiveConfig.bridge.groupsRaw.map { it.name } }
                        literal("add-block") {
                            argument("block", BlockPredicateArgument.blockPredicate(ctx)) {
                                executesAsync {
                                    val name = StringArgumentType.getString(this, "group")
                                    source.editContent(name, getRaw(5), true, true)
                                }
                            }
                        }
                        literal("remove-block") {
                            argument("block", BlockPredicateArgument.blockPredicate(ctx)) {
                                suggestStrings { c ->
                                    val group = c.getArgument("group", String::class.java)
                                    c.suggestFilteredList(groupExists(group)?.blocks)
                                }
                                executesAsync {
                                    val name = StringArgumentType.getString(this, "group")
                                    source.editContent(name, getRaw(5), true, false)
                                }
                            }
                        }
                        literal("add-tool") {
                            argument("tool", ItemPredicateArgument.itemPredicate(ctx)) {
                                executesAsync {
                                    val name = StringArgumentType.getString(this, "group")
                                    source.editContent(name, getRaw(5), false, true)
                                }
                            }
                        }
                        literal("remove-tool") {
                            argument("tool", ItemPredicateArgument.itemPredicate(ctx)) {
                                suggestStrings { c ->
                                    val group = c.getArgument("group", String::class.java)
                                    c.suggestFilteredList(groupExists(group)?.tools)
                                }
                                executesAsync {
                                    val name = StringArgumentType.getString(this, "group")
                                    source.editContent(name, getRaw(5), false, false)
                                }
                            }
                        }
                        literal("override") {
                            overrides { name -> groupExists(name)?.override }
                        }
                    }
                }
            }

            literal("presets") {
                requiresPermission(permissionGroups)
                executesAsync { source.msg("Quick add preconfigured groups & settings", cBase) }
                fun CommandNodeBuilder<CommandSourceStack>.addPreset(name: String, blocks: Set<String>, tools: Set<String>, override: VeinminerSettingsOverride = VeinminerSettingsOverride()) {
                    literal(name) {
                        executesAsync {
                            if (ActiveConfig.bridge.groupsRaw.any { it.name.equals(name, ignoreCase = true) }) {
                                return@executesAsync source.msg("Group '$name' already exists", cRed)
                            }
                            ActiveConfig.bridge.groupsRaw.add(BlockGroup(name, blocks.toMutableSet(), tools.toMutableSet(), override))
                            source.msg("Added preset group '$name'\n - Blocks: $blocks\n - Tools: $tools", cGreen)
                            ActiveConfig.bridge.save()
                        }
                    }
                }

                addPreset("Logs", mutableSetOf("#minecraft:logs", "minecraft:mushroom_stem", "minecraft:brown_mushroom_block", "minecraft:red_mushroom_block"), mutableSetOf("#minecraft:axes"))
                addPreset("Leaves", mutableSetOf("#minecraft:leaves"), mutableSetOf("#minecraft:hoes", "minecraft:shears"))
                addPreset("Crops", mutableSetOf("#minecraft:crops"), mutableSetOf())
                addPreset("ConcretePowder", mutableSetOf("#minecraft:concrete_powder"), mutableSetOf("#minecraft:shovels"))
                addPreset("Terracotta", mutableSetOf("#minecraft:terracotta"), mutableSetOf("#minecraft:pickaxes"))
                addPreset("Grassy", mutableSetOf("#minecraft:edible_for_sheep", "minecraft:tall_grass", "minecraft:large_fern", "minecraft:bush", "minecraft:dead_bush"), mutableSetOf(), VeinminerSettingsOverride(searchRadius = 3, maxChain = 50))
                addPreset("Stones", mutableSetOf("#minecraft:base_stone_overworld", "minecraft:cobblestone", "minecraft:cobbled_deepslate"), mutableSetOf("#minecraft:pickaxes"))
                addPreset("NetherStones", mutableSetOf("#minecraft:base_stone_nether", "minecraft:smooth_basalt"), mutableSetOf("#minecraft:pickaxes"))
            }
        }

    private inline fun <reified T> CommandNodeBuilder<CommandSourceStack>.applySetting(
        name: String,
        noinline currentConsumer: (CommandContext<CommandSourceStack>) -> T,
        noinline consumer: (T, CommandContext<CommandSourceStack>) -> Unit,
    ) {
        literal(name) {
            executesAsync {
                val currentString = currentConsumer.invoke(this)?.toString() ?: "unset"
                source.msg("$name is currently set to $currentString", cBase)
            }

            when (typeOf<T>()) {
                typeOf<Boolean>(), typeOf<Boolean?>() -> boolArg("$name-new") {
                    executesAsync {
                        val value = BoolArgumentType.getBool(this, "$name-new") as T
                        consumer.invoke(value, this)
                        ActiveConfig.bridge.save()
                        source.msg("$name set to $value", cGreen)
                    }
                }

                typeOf<Int>(), typeOf<Int?>() -> intArg("$name-new", min = 0) {
                    executesAsync {
                        val value = IntegerArgumentType.getInteger(this, "$name-new") as T
                        consumer.invoke(value, this)
                        ActiveConfig.bridge.save()
                        source.msg("$name set to $value", cGreen)
                    }
                }

                typeOf<Double>(), typeOf<Double?>() -> doubleArg("$name-new", min = 0.0, max = 1.0) {
                    executesAsync {
                        val value = DoubleArgumentType.getDouble(this, "$name-new") as T
                        consumer.invoke(value, this)
                        ActiveConfig.bridge.save()
                        source.msg("$name set to $value", cGreen)
                    }
                }
            }
        }
    }

    private fun CommandSourceStack.msg(message: String, color: Int) {
        try {
            sendSystemMessage(Component.literal(message).withColor(color))
        } catch (_: Exception) {
            try {
                sendSystemMessage(Component.literal(message))
            } catch (_: Exception) {
                ActiveHost.host.logger.warn("Messages cannot be sent in this version")
            }
        }
    }

    private fun CommandContext<CommandSourceStack>.suggestFilteredList(list: Collection<String>?): List<String> {
        val input = input.split(' ').lastOrNull() ?: ""
        return list?.filter { input.isEmpty() || it.startsWith(input) } ?: emptyList()
    }

    private fun CommandContext<CommandSourceStack>.getRaw(idx: Int) = input.split(' ').getOrNull(idx)

    private fun CommandNodeBuilder<CommandSourceStack>.overrides(
        resolver: (String) -> VeinminerSettingsOverride?,
    ) {
        fun CommandContext<CommandSourceStack>.resolve(): VeinminerSettingsOverride {
            val name = runCatching { getArgument("group", String::class.java) }.getOrNull() ?: "client"
            val override = resolver.invoke(name)
            return if (override == null) {
                ActiveHost.host.logger.warn("Tried to access override for '${name}', but it does not exist. Changes are not saved!")
                VeinminerSettingsOverride()
            } else override
        }

        literal("unset") {
            stringArg("key") {
                suggestStrings { c -> c.resolve().nonNullKeys() }
                executesAsync {
                    val key = StringArgumentType.getString(this, "key")
                    if (resolve().unset(key)) {
                        source.msg("Unset override '$key'", cGreen)
                        ActiveConfig.bridge.save()
                    } else {
                        source.msg("Override '$key' is not set", cRed)
                    }
                }
            }
        }

        applySetting("cooldown", { args -> args.resolve().cooldown }) { x, args -> args.resolve().cooldown = x }
        applySetting("mustSneak", { args -> args.resolve().mustSneak }) { x, args -> args.resolve().mustSneak = x }
        applySetting("delay", { args -> args.resolve().delay }) { x, args -> args.resolve().delay = x }
        applySetting("maxChain", { args -> args.resolve().maxChain }) { x, args -> args.resolve().maxChain = x }
        applySetting("needCorrectTool", { args -> args.resolve().needCorrectTool }) { x, args -> args.resolve().needCorrectTool = x }
        applySetting("searchRadius", { args -> args.resolve().searchRadius }) { x, args -> args.resolve().searchRadius = x }
        applySetting("permissionRestricted", { args -> args.resolve().permissionRestricted }) { x, args -> args.resolve().permissionRestricted = x }
        applySetting("decreaseDurability", { args -> args.resolve().decreaseDurability }) { x, args -> args.resolve().decreaseDurability = x }
        applySetting("miningSpeedModifier", { args -> args.resolve().miningSpeedModifier }) { x, args -> args.resolve().miningSpeedModifier = x }
    }
}
