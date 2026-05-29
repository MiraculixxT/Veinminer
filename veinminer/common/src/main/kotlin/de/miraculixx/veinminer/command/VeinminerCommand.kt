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
import de.miraculixx.veinminer.utils.cHighlight
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.cWhite
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
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.net.URI
import kotlin.reflect.typeOf

object VeinminerCommand {

    fun build(ctx: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> =
        veinminerCommand("veinminer") {
            executesAsync {
                val host = ActiveHost.host
                source.sendSystemMessage(
                    header("Veinminer") + cmp("\nVersion: ") + value(host.versionVeinminer) +
                        cmp(" (") + value(host.platform) + cmp(" - ") + value(host.versionMinecraft) + cmp(")") +
                        cmp("\nDownload: ") + link("Modrinth", "https://modrinth.com/project/veinminer") +
                        cmp(" | ") + link("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/veinminer-mod")
                )
            }

            literal("reload") {
                requiresPermission(permissionReload)
                executesAsync {
                    ActiveConfig.bridge.reload(true)
                    source.sendSystemMessage(success("Veinminer config reloaded"))
                }
            }

            literal("blocks") {
                requiresPermission(permissionBlocks)
                executesAsync { source.sendSystemMessage(usage("/veinminer blocks <add/remove> <block>")) }

                literal("add") {
                    argument("block", BlockPredicateArgument.blockPredicate(ctx)) {
                        executesAsync {
                            val id = getRaw(3) ?: return@executesAsync source.sendSystemMessage(error("Block can not be null"))
                            if (ActiveConfig.bridge.veinBlocksRaw.add(id)) {
                                ActiveConfig.bridge.save()
                                source.sendSystemMessage(success("Added ") + value(id) + success(" to veinminer blocks"))
                            } else {
                                source.sendSystemMessage(value(id) + error(" is already a veinminer block"))
                            }
                        }
                    }
                }

                literal("remove") {
                    argument("block", BlockPredicateArgument.blockPredicate(ctx)) {
                        suggestStrings { c -> c.suggestFilteredList(ActiveConfig.bridge.veinBlocksRaw) }
                        executesAsync {
                            val id = getRaw(3) ?: return@executesAsync source.sendSystemMessage(error("Block can not be null"))
                            if (ActiveConfig.bridge.veinBlocksRaw.remove(id)) {
                                ActiveConfig.bridge.save()
                                source.sendSystemMessage(success("Removed ") + value(id) + success(" from veinminer blocks"))
                            } else {
                                source.sendSystemMessage(value(id) + error(" is not a veinminer block"))
                            }
                        }
                    }
                }
            }

            literal("toggle") {
                requiresPermission(permissionToggle)
                executesAsync {
                    if (ActiveHost.host.active) source.sendSystemMessage(error("Veinminer functions disabled"))
                    else source.sendSystemMessage(success("Veinminer functions enabled"))
                    ActiveHost.host.active = !ActiveHost.host.active
                }
            }

            literal("settings") {
                requiresPermission(permissionSettings)
                executesAsync { source.sendSystemMessage(usage("/veinminer settings <setting> [<new-value>]")) }

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
                        sendSystemMessage(error("Group ") + value(name) + error(" already exists"))
                        return
                    }
                    ActiveConfig.bridge.groupsRaw.add(BlockGroup(name, content))
                    sendSystemMessage(
                        success("Created group ") + value(name) +
                            cmp("\nAdd blocks with ", cBase) +
                            command("/veinminer groups edit $name add-block <block>")
                    )
                    ActiveConfig.bridge.save()
                }

                fun CommandSourceStack.editContent(groupName: String, rawKey: String?, isBlock: Boolean, isAdd: Boolean) {
                    val group = groupExists(groupName) ?: return sendSystemMessage(error("Group ") + value(groupName) + error(" does not exist"))
                    val set = if (isBlock) group.blocks else group.tools
                    if (rawKey == null) return sendSystemMessage(error("Invalid material"))
                    val label = if (isBlock) "block" else "tool"
                    if (isAdd) {
                        if (set.add(rawKey)) sendSystemMessage(success("Added $label ") + value(rawKey) + success(" to group ") + value(groupName))
                        else return sendSystemMessage(value(rawKey) + error(" is already in group ") + value(groupName))
                    } else {
                        if (set.remove(rawKey)) sendSystemMessage(success("Removed $label ") + value(rawKey) + success(" from group ") + value(groupName))
                        else return sendSystemMessage(value(rawKey) + error(" is not in group ") + value(groupName))
                    }
                    ActiveConfig.bridge.save()
                }

                executesAsync {
                    source.sendSystemMessage(
                        header("Groups") +
                            cmp("\nChain multiple block types into one veinmine target.", cBase) +
                            cmp("\nExample: ", cBase) + value("Ores") + cmp(" with ", cBase) + value("iron_ore") +
                            cmp(" and ", cBase) + value("deepslate_iron_ore") +
                            cmp("\nGroups can be limited to certain tools.", cBase)
                    )
                }

                literal("list") {
                    fun BlockGroup<String>.print(source: CommandSourceStack) {
                        source.sendSystemMessage(
                            header("Group ") + value(name) +
                                cmp("\nBlocks: ", cBase) + list(blocks) +
                                cmp("\nTools: ", cBase) + list(tools, "all")
                        )
                    }

                    stringArg("group") {
                        suggestStrings { ActiveConfig.bridge.groupsRaw.map { it.name } }
                        executesAsync {
                            val name = StringArgumentType.getString(this, "group")
                            val group = groupExists(name) ?: return@executesAsync source.sendSystemMessage(error("Group ") + value(name) + error(" does not exist"))
                            group.print(source)
                        }
                    }

                    executesAsync {
                        val groups = ActiveConfig.bridge.groupsRaw
                        if (groups.isEmpty()) source.sendSystemMessage(cmp("No groups configured"))
                        else groups.forEach { group -> group.print(source) }
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
                            val group = groupExists(name) ?: return@executesAsync source.sendSystemMessage(error("Group ") + value(name) + error(" does not exist"))
                            ActiveConfig.bridge.groupsRaw.remove(group)
                            source.sendSystemMessage(success("Removed group ") + value(name))
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
                executesAsync { source.sendSystemMessage(cmp("Quick add preconfigured groups and settings")) }
                fun CommandNodeBuilder<CommandSourceStack>.addPreset(name: String, blocks: Set<String>, tools: Set<String>, override: VeinminerSettingsOverride = VeinminerSettingsOverride()) {
                    literal(name) {
                        executesAsync {
                            if (ActiveConfig.bridge.groupsRaw.any { it.name.equals(name, ignoreCase = true) }) {
                                return@executesAsync source.sendSystemMessage(error("Group ") + value(name) + error(" already exists"))
                            }
                            ActiveConfig.bridge.groupsRaw.add(BlockGroup(name, blocks.toMutableSet(), tools.toMutableSet(), override))
                            source.sendSystemMessage(
                                success("Added preset group ") + value(name) +
                                    cmp("\nBlocks: ", cBase) + list(blocks) +
                                    cmp("\nTools: ", cBase) + list(tools, "all")
                            )
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
                source.sendSystemMessage(setting(name, currentString))
            }

            when (typeOf<T>()) {
                typeOf<Boolean>(), typeOf<Boolean?>() -> boolArg("$name-new") {
                    executesAsync {
                        val value = BoolArgumentType.getBool(this, "$name-new") as T
                        consumer.invoke(value, this)
                        ActiveConfig.bridge.save()
                        source.sendSystemMessage(settingChanged(name, value))
                    }
                }

                typeOf<Int>(), typeOf<Int?>() -> intArg("$name-new", min = 0) {
                    executesAsync {
                        val value = IntegerArgumentType.getInteger(this, "$name-new") as T
                        consumer.invoke(value, this)
                        ActiveConfig.bridge.save()
                        source.sendSystemMessage(settingChanged(name, value))
                    }
                }

                typeOf<Double>(), typeOf<Double?>() -> doubleArg("$name-new", min = 0.0, max = 1.0) {
                    executesAsync {
                        val value = DoubleArgumentType.getDouble(this, "$name-new") as T
                        consumer.invoke(value, this)
                        ActiveConfig.bridge.save()
                        source.sendSystemMessage(settingChanged(name, value))
                    }
                }
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
                        source.sendSystemMessage(success("Unset override ") + value(key))
                        ActiveConfig.bridge.save()
                    } else {
                        source.sendSystemMessage(error("Override ") + value(key) + error(" is not set"))
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

    private fun header(text: String) = cmp(text, cGreen, bold = true)
    private fun success(text: String) = cmp(text, cGreen)
    private fun error(text: String) = cmp(text, cRed)
    private fun value(value: Any?) = cmp(value.toString(), cGreen)
    private fun command(text: String) = cmp(text, cGreen).suggest(text).hover(cmp("Click to suggest command", cWhite))
            fun link(name: String, url: String) = cmp(name, cHighlight, true).link(url).hover(cmp("Click to open URL", cWhite))
    private fun usage(syntax: String) = error("Correct Syntax: ") + command(syntax).withColor(cBase)
    private fun setting(name: String, current: String) = cmp(name) + cmp(" is currently set to ", cBase) + value(current)
    private fun settingChanged(name: String, newValue: Any?) = success(name) + success(" set to ") + value(newValue)

    private fun list(values: Collection<String>, empty: String = "none"): MutableComponent {
        if (values.isEmpty()) return value(empty)
        val component = cmp("[", cBase)
        values.forEachIndexed { index, item ->
            if (index > 0) component.append(cmp(", ", cBase))
            component.append(value(item))
        }
        return component.append(cmp("]", cBase))
    }

    private fun cmp(text: String, color: Int = cBase, bold: Boolean = false, italic: Boolean = false) =
        Component.literal(text).withColor(color).withStyle { it.withBold(bold).withItalic(italic) }
    private operator fun MutableComponent.plus(other: Component) = this.append(other)
    private fun MutableComponent.suggest(command: String) = this.withStyle { it.withClickEvent(ClickEvent.SuggestCommand(command)) }
    private fun MutableComponent.link(url: String) = this.withStyle { it.withClickEvent(ClickEvent.OpenUrl(URI(url))) }
    private fun MutableComponent.hover(cmp: Component) = this.withStyle { it.withHoverEvent(HoverEvent.ShowText(cmp)) }
}
