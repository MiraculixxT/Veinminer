package de.miraculixx.veinminerClient.mining

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.pattern.BlockAwareness
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.pattern.VeinmineAction
import de.miraculixx.veinminer.pattern.Veinmining
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * Client copy of veinmine check.
 * If server & client desync, server takes authority and client just see ghost highlights
 */
object ClientVeinSelector {

    data class Result(val positions: List<BlockPosition>, val toolIcon: String)

    // Future me, don't try to unify with server check. Not worth it
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
            val hasEnchant = mainHandItem.enchantments.keySet().any { it.`is`(enchantKey) }
            if (!hasEnchant) return null
        }

        val targets = if (isGroupBlock) blockGroup.blocks else setOf(material)
        val originPos = BlockPosition(origin.x, origin.y, origin.z)
        val blockAwareness = object : BlockAwareness {
            override fun getBlockType(pos: BlockPosition): Identifier {
                return level.getBlockState(BlockPos(pos.x, pos.y, pos.z)).key()
            }

            override fun breakBlock(pos: BlockPosition, ticks: Int): Boolean {
                return false
            }
        }

        val action = VeinmineAction(
            currentBlock = originPos,
            targetTypes = targets,
            tool = mainHandItem,
            player = player,
            sourceLocation = originPos,
            settings = settings,
            face = face
        )
        val hits = Veinmining.veinmine(action, blockAwareness, shape, maxDepth, false)
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
}
