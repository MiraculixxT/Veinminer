@file:Suppress("unused")

package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.mcCoroutineSync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.pattern.VeinSelector
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

/**
 * Used by NMS-based loaders (Fabric, NeoForge). Paper has its own Bukkit-API-based
 * implementation in `veinminer-paper` and intentionally does not use this one.
 *
 * Loaders must populate [EventState] during init and register their own
 * block-break / attack-block hooks that delegate to [allowedToVeinmine] + [veinmine].
 */
object VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()
    private val speedModifierId: Identifier = Identifier.fromNamespaceAndPath("veinminer", "veinmine_speed")

    private fun Identifier.groupedBlocks(): FixedBlockGroup<Identifier> {
        val blocks = mutableSetOf<Identifier>()
        val tools = mutableSetOf<Identifier>()
        var override: VeinminerSettingsOverride? = null

        EventState.configManager.groups.forEach {
            if (it.blocks.contains(this)) {
                if (override == null) override = it.override
                blocks.addAll(it.blocks)
                tools.addAll(it.tools)
            }
        }
        return FixedBlockGroup(blocks.toSet(), tools.toSet(), override)
    }

    fun BlockState.key(): Identifier = block.key()
    fun Block.key(): Identifier = BuiltInRegistries.BLOCK.getKey(this)
    fun ItemStack.key(): Identifier = BuiltInRegistries.ITEM.getKey(item)

    fun getPreferredToolIcon(block: BlockState): String = when {
        block.`is`(BlockTags.MINEABLE_WITH_AXE) -> "axe"
        block.`is`(BlockTags.MINEABLE_WITH_SHOVEL) -> "shovel"
        block.`is`(BlockTags.MINEABLE_WITH_HOE) -> "hoe"
        else -> "pickaxe"
    }

    /**
     * Apply the per-block-break attribute speed modifier when a vein chain is
     * detected on attack. Returns true if a modifier was applied (caller may
     * inspect to decide further behavior).
     */
    fun applySpeedModifierOnAttack(world: Level, player: Player, pos: BlockPos, state: BlockState) {
        if (world.isClientSide) return
        player.removeMiningSpeedModifier()

        val info = allowedToVeinmine(world, player, pos, state) ?: return
        val multiplicator = info.settings.miningSpeedModifier
        if (multiplicator <= 0.0) return

        val amount = info.veinmine(false)
        if (amount <= 1) return

        val modifier = info.settings.calculateBreakSpeedModifier(amount, multiplicator)
        val attribute = player.getAttribute(Attributes.BLOCK_BREAK_SPEED) ?: return
        attribute.removeModifier(speedModifierId)
        attribute.addTransientModifier(AttributeModifier(speedModifierId, modifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
    }

    /**
     * Hook for the loader's block-break-before event. Returns true if the break
     * should proceed, false to cancel.
     */
    fun onBlockBreakBefore(world: Level, player: Player, pos: BlockPos, state: BlockState): Boolean {
        player.removeMiningSpeedModifier()
        val info = allowedToVeinmine(world, player, pos, state) ?: return true

        val amount = info.veinmine(true)
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(state.block), amount - 1)

        val cooldownTime = info.settings.cooldown
        if (cooldownTime > 0) {
            cooldown.add(player.uuid)
            mcCoroutineAsync(cooldownTime.ticks) { cooldown.remove(player.uuid) }
        }
        return true
    }

    /**
     * @return the veinmine action if the player is allowed to veinmine the block, null otherwise
     */
    fun allowedToVeinmine(world: Level, player: Player, pos: BlockPos, state: BlockState): VeinmineAction? {
        if (!ActiveHost.host.active) return null

        if (player.gameMode() == GameType.CREATIVE) return null

        val uuid = player.uuid
        val hasClient = NetworkRouter.registeredPlayers.contains(uuid)
        val material = state.key().takeIf { !state.isAir } ?: return null

        if (hasClient && !NetworkRouter.isReady(uuid)) return null

        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val settings = EventState.configManager.settings.applyOverrides(hasClient, blockGroup.override)

        if (settings.permissionRestricted && !EventState.checkPermission(player, permissionVeinmine)) return null
        val hasClientBypass = settings.client.allBlocks && NetworkRouter.registeredPlayers.containsKey(player.uuid)
        val isWhitelisted = isGroupBlock || EventState.configManager.veinBlocks.contains(material)

        if (settings.client.require && !hasClient) return null
        if (!isWhitelisted && !hasClientBypass) return null
        if (settings.mustSneak && !player.isCrouching) return null
        if (cooldown.contains(player.uuid)) return null

        val mainHandItem = player.mainHandItem
        if (settings.needCorrectTool && (state.requiresCorrectToolForDrops() && !mainHandItem.isCorrectToolForDrops(state))) return null
        if (!hasClientBypass && isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(mainHandItem.key())) return null
        if (settings.decreaseDurability && mainHandItem.remainingDurability() <= 1) return null

        if (EventState.enchantmentActive && !mainHandItem.enchantments.keySet().any { it.`is`(EventState.enchantmentKey) }) return null

        val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
        val face = NetworkRouter.lastSurface[player.uuid] ?: Surface.UP
        return VeinmineAction(state, pos, blocks, mainHandItem, mutableSetOf(), player, world, pos, settings, face)
    }

    /**
     * Recursively break blocks around the source block until the vein stops.
     * @return the number of blocks broken
     */
    fun VeinmineAction.veinmine(shouldBreak: Boolean): Int {
        if (tool.isEmpty) return 0
        if (!targetTypes.contains(currentBlock.key())) return 0
        val shape = NetworkRouter.activeShape(player.uuid) ?: Shape.NORMAL
        return if (shape == Shape.NORMAL) floodFillMine(shouldBreak)
        else shapeMine(shape, shouldBreak)
    }

    private fun VeinmineAction.floodFillMine(shouldBreak: Boolean): Int {
        val targets = targetTypes
        val visited = processedBlocks
        val decreaseDurability = settings.decreaseDurability
        val delay = settings.delay
        val originPos = BlockPosition(currentPosition.x, currentPosition.y, currentPosition.z)

        val hits = VeinSelector.floodFill(
            origin = originPos,
            isMatch = { p -> targets.contains(world.getBlockState(BlockPos(p.x, p.y, p.z)).key()) },
            maxChain = settings.maxChain,
            searchRadius = settings.searchRadius,
        )
        for (hit in hits) {
            val pos = BlockPos(hit.pos.x, hit.pos.y, hit.pos.z)
            if (hit.distance > 0 && shouldBreak) {
                if (decreaseDurability && tool.remainingDurability() <= 1) {
                    visited.add(pos)
                    continue
                }
                val state = world.getBlockState(pos)
                scheduleBreak(state, pos, hit.distance, decreaseDurability, delay, targets)
            }
            visited.add(pos)
        }
        return visited.size
    }

    private fun VeinmineAction.shapeMine(shape: Shape, shouldBreak: Boolean): Int {
        val targets = targetTypes
        val visited = processedBlocks
        val decreaseDurability = settings.decreaseDurability
        val delay = settings.delay
        val origin = BlockPosition(currentPosition.x, currentPosition.y, currentPosition.z)
        val maxDepth = NetworkRouter.maxDepth(player.uuid)

        val hits = VeinSelector.shapeFill(
            origin = origin,
            face = face,
            shape = shape,
            isMatch = { p -> targets.contains(world.getBlockState(BlockPos(p.x, p.y, p.z)).key()) },
            maxChain = settings.maxChain,
            searchRadius = settings.searchRadius,
            maxDepth = maxDepth,
        )
        for (hit in hits) {
            val pos = BlockPos(hit.pos.x, hit.pos.y, hit.pos.z)
            if (hit.distance > 0 && shouldBreak) {
                if (decreaseDurability && tool.remainingDurability() <= 1) {
                    visited.add(pos)
                    continue
                }
                val state = world.getBlockState(pos)
                scheduleBreak(state, pos, hit.distance, decreaseDurability, delay, targets)
            }
            visited.add(pos)
        }
        return visited.size
    }

    private fun VeinmineAction.scheduleBreak(
        state: BlockState,
        pos: BlockPos,
        distance: Int,
        decreaseDurability: Boolean,
        delay: Int,
        targets: Set<Identifier>
    ) {
        mcCoroutineSync(mcServer!!, delay * distance) {
            if (delay != 0 && !targets.contains(state.key())) return@mcCoroutineSync
            if (decreaseDurability && tool.remainingDurability() <= 1) return@mcCoroutineSync
            state.destroyBlock(tool, world, pos, player, sourceLocation)
            if (decreaseDurability) damageItem(tool, player)
        }
    }

    private fun BlockState.destroyBlock(
        item: ItemStack,
        world: Level,
        position: BlockPos,
        player: Player,
        initialSource: BlockPos
    ) {
        val block = block
        if (block !== Blocks.AIR && (!requiresCorrectToolForDrops() || item.isCorrectToolForDrops(this))) {
            improvedDropResources(this, world, position, world.getBlockEntity(position), player, item, initialSource)

            if (block is BaseFireBlock) {
                world.levelEvent(1009, position, 0)
            } else {
                world.levelEvent(2001, position, Block.getId(this))
            }
        }

        val destroyed = world.removeBlock(position, false)
        if (destroyed) {
            block.destroy(world, position, this)
        }
    }

    private fun improvedDropResources(
        blockState: BlockState,
        world: Level,
        blockPos: BlockPos,
        blockEntity: BlockEntity?,
        breaker: Entity,
        tool: ItemStack,
        initialSource: BlockPos
    ) {
        val serverLevel = world as? ServerLevel ?: return
        Block.getDrops(blockState, serverLevel, blockPos, blockEntity, breaker, tool).forEach { drop: ItemStack ->
            val dropPos = if (EventState.configManager.settings.mergeItemDrops) initialSource else blockPos
            Block.popResource(world, dropPos, drop)
        }
        blockState.spawnAfterBreak(serverLevel, blockPos, tool, true)
    }

    private fun damageItem(item: ItemStack, player: Player) {
        if (item.isEmpty) return
        item.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
    }

    private fun ItemStack.remainingDurability(): Int {
        if (isEmpty) return 0
        if (maxDamage <= 0) return Int.MAX_VALUE
        return maxDamage - damageValue
    }

    fun Player.removeMiningSpeedModifier() {
        getAttribute(Attributes.BLOCK_BREAK_SPEED)?.removeModifier(speedModifierId)
    }

    data class VeinmineAction(
        val currentBlock: BlockState,
        val currentPosition: BlockPos,
        val targetTypes: Set<Identifier>,
        val tool: ItemStack,
        val processedBlocks: MutableSet<BlockPos>,
        val player: Player,
        val world: Level,
        val sourceLocation: BlockPos,
        val settings: VeinminerSettings,
        val face: Surface
    )

}
