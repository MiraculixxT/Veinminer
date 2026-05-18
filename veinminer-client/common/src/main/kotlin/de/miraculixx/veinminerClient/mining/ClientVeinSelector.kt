package de.miraculixx.veinminerClient.mining

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.pattern.VeinSelector
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * Client-side mirror of the server's vein-mine gate + selection. Runs against the local
 * `ClientLevel` to drive the highlight overlay without a server round-trip. The server
 * stays authoritative on the actual break — false positives here just render a phantom
 * highlight that the server will silently ignore on swing.
 */
object ClientVeinSelector {

    data class Result(val positions: List<BlockPosition>, val toolIcon: String)

    fun resolve(
        level: Level,
        player: LocalPlayer,
        origin: BlockPos,
        face: Surface,
        shape: Shape,
        maxDepth: Int,
    ): Result? {
        if (!NetworkManager.isVeinminerActive) return null
        if (!NetworkManager.hostActive) return null
        if (player.gameMode() == GameType.CREATIVE) return null

        val state = level.getBlockState(origin)
        val material = state.key().takeIf { !state.isAir } ?: return null

        val blockGroup = groupedBlocks(material)
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val settings = NetworkManager.settings.applyOverrides(isClient = true, g = blockGroup.override)

        if (settings.permissionRestricted && !NetworkManager.hasUsePermission) return null
        val hasClientBypass = settings.client.allBlocks
        val isWhitelisted = isGroupBlock || NetworkManager.veinBlocks.contains(material)
        if (!isWhitelisted && !hasClientBypass) return null
        if (settings.mustSneak && !player.isCrouching) return null

        val mainHandItem = player.mainHandItem
        if (settings.needCorrectTool && state.requiresCorrectToolForDrops() && !mainHandItem.isCorrectToolForDrops(state)) return null
        if (!hasClientBypass && isGroupBlock && blockGroup.tools.isNotEmpty() && !blockGroup.tools.contains(mainHandItem.key())) return null
        if (settings.decreaseDurability && mainHandItem.remainingDurability() <= 1) return null

        val enchantKey = NetworkManager.enchantmentKey
        if (NetworkManager.enchantmentActive && enchantKey != null) {
            val hasEnchant = mainHandItem.enchantments.keySet().any { it.identifier() == enchantKey }
            if (!hasEnchant) return null
        }

        val targets = if (isGroupBlock) blockGroup.blocks else setOf(material)
        val originPos = BlockPosition(origin.x, origin.y, origin.z)
        val match: (BlockPosition) -> Boolean = { p ->
            targets.contains(level.getBlockState(BlockPos(p.x, p.y, p.z)).key())
        }

        val hits = if (shape == Shape.NORMAL) {
            VeinSelector.floodFill(originPos, match, settings.maxChain, settings.searchRadius)
        } else {
            VeinSelector.shapeFill(originPos, face, shape, match, settings.maxChain, settings.searchRadius, maxDepth)
        }
        return Result(hits.map { it.pos }, preferredToolIcon(state))
    }

    private fun groupedBlocks(material: Identifier): FixedBlockGroup<Identifier> {
        val blocks = mutableSetOf<Identifier>()
        val tools = mutableSetOf<Identifier>()
        var override: VeinminerSettingsOverride? = null
        NetworkManager.groups.forEach { g ->
            if (g.blocks.contains(material)) {
                if (override == null) override = g.override
                blocks.addAll(g.blocks)
                tools.addAll(g.tools)
            }
        }
        return FixedBlockGroup(blocks.toSet(), tools.toSet(), override)
    }

    private fun preferredToolIcon(state: BlockState): String = when {
        state.`is`(BlockTags.MINEABLE_WITH_AXE) -> "axe"
        state.`is`(BlockTags.MINEABLE_WITH_SHOVEL) -> "shovel"
        state.`is`(BlockTags.MINEABLE_WITH_HOE) -> "hoe"
        else -> "pickaxe"
    }

    private fun BlockState.key(): Identifier = BuiltInRegistries.BLOCK.getKey(block)
    private fun ItemStack.key(): Identifier = BuiltInRegistries.ITEM.getKey(item)
    private fun ItemStack.remainingDurability(): Int {
        if (isEmpty) return 0
        if (maxDamage <= 0) return Int.MAX_VALUE
        return maxDamage - damageValue
    }

    private fun Player.gameMode(): GameType {
        val mc = Minecraft.getInstance()
        val info = mc.connection?.getPlayerInfo(uuid)
        return info?.gameMode ?: GameType.SURVIVAL
    }
}
