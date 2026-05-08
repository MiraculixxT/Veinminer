@file:Suppress("unused")

package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.extensions.mcCoroutineDelay
import de.miraculixx.veinminer.extensions.mcScheduleDelay
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminer.network.NetworkRouter
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
import java.util.LinkedList
import java.util.Queue
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

        mcCoroutineDelay(info.settings.delay.ticks) {
            val amount = info.veinmine(true)
            player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(state.block), amount - 1)
        }

        val cooldownTime = info.settings.cooldown
        if (cooldownTime > 0) {
            cooldown.add(player.uuid)
            mcCoroutineDelay(cooldownTime.ticks) { cooldown.remove(player.uuid) }
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

        if (hasClient && !NetworkRouter.readyToVeinmine.contains(uuid)) return null

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
        return VeinmineAction(state, pos, blocks, mainHandItem, mutableSetOf(), player, world, pos, settings)
    }

    /**
     * Recursively break blocks around the source block until the vein stops.
     * @return the number of blocks broken
     */
    fun VeinmineAction.veinmine(shouldBreak: Boolean): Int {
        val queue: Queue<VeinmineBlock> = LinkedList()
        queue.add(VeinmineBlock(currentBlock, currentPosition, 0))

        while (queue.isNotEmpty()) {
            val vBlock = queue.poll()
            val pos = vBlock.position
            if (!targetTypes.contains(vBlock.block.key()) || processedBlocks.contains(pos)) continue
            val size = processedBlocks.size
            if (size >= settings.maxChain) continue
            if (tool.isEmpty) continue

            if (size != 0 && shouldBreak) {
                if (settings.decreaseDurability && tool.remainingDurability() <= 1) continue
                mcScheduleDelay(mcServer!!, settings.delay * vBlock.distance) {
                    if (settings.delay != 0) {
                        if (!targetTypes.contains(vBlock.block.key())) return@mcScheduleDelay
                    }
                    if (settings.decreaseDurability && tool.remainingDurability() <= 1) return@mcScheduleDelay

                    vBlock.block.destroyBlock(tool, world, pos, player, sourceLocation)
                    if (settings.decreaseDurability) damageItem(tool, player)
                }
            }
            processedBlocks.add(pos)

            val searchRadius = settings.searchRadius
            (-searchRadius..searchRadius).forEach { x ->
                (-searchRadius..searchRadius).forEach { y ->
                    (-searchRadius..searchRadius).forEach z@{ z ->
                        if (x == 0 && y == 0 && z == 0) return@z
                        val newPos = BlockPos(pos.x + x, pos.y + y, pos.z + z)
                        val newBlock = world.getBlockState(newPos)
                        queue.add(VeinmineBlock(newBlock, newPos, vBlock.distance + 1))
                    }
                }
            }
        }
        return processedBlocks.size
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
        val settings: VeinminerSettings
    )

    data class VeinmineBlock(
        val block: BlockState,
        val position: BlockPos,
        val distance: Int
    )
}
