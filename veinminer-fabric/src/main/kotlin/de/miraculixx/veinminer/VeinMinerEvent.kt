package de.miraculixx.veinminer

import de.miraculixx.veinminer.Veinminer.Companion.VEINMINE
import de.miraculixx.veinminer.Veinminer.Companion.active
import de.miraculixx.veinminer.Veinminer.Companion.enchantmentActive
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.FixedBlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.utils.permissionVeinmine
import de.miraculixx.veinminer.networking.FabricNetworking
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import java.util.*

object VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()

    /**
     * @return a set of all blocks in the same group as this material. If the material is not in a group, it will return an empty set
     */
    private fun ResourceLocation.groupedBlocks(): FixedBlockGroup<ResourceLocation> {
        val blocks = mutableSetOf<ResourceLocation>()
        val tools = mutableSetOf<ResourceLocation>()
        ConfigManager.groups.forEach {
            if (it.blocks.contains(this)) {
                blocks.addAll(it.blocks); tools.addAll(it.tools)
            }
        }
        return FixedBlockGroup(blocks.toSet(), tools.toSet())
    }

    fun BlockState.key() = block.key()
    fun Block.key() = BuiltInRegistries.BLOCK.getKey(this)
    fun ItemStack.key() = BuiltInRegistries.ITEM.getKey(item)

    private val event = PlayerBlockBreakEvents.BEFORE.register { world, player, pos, state, _ ->
        val veinmineInfo = allowedToVeinmine(world, player, pos, state)
        if (veinmineInfo == null) return@register true

        val settings = ConfigManager.settings
        val uuid = player.uuid

        // Check if player has the client mod and pressed the key
        if (FabricNetworking.registeredPlayers.contains(uuid) && !FabricNetworking.readyToVeinmine.contains(uuid)) return@register true

        mcCoroutineTask(delay = settings.delay.ticks) {
            veinmineInfo.veinmine(true)
        }

        // Check for cooldown config
        val cooldownTime = settings.cooldown
        if (cooldownTime > 0) {
            cooldown.add(player.uuid)

            mcCoroutineTask(delay = cooldownTime.ticks) {
                cooldown.remove(player.uuid)
            }
        }

        return@register true
    }

    /**
     * Get the preferred tool icon for the block
     */
    fun getPreferredToolIcon(block: BlockState): String {
        return when {
            block.`is`(BlockTags.MINEABLE_WITH_AXE) -> "axe"
            block.`is`(BlockTags.MINEABLE_WITH_SHOVEL) -> "shovel"
            block.`is`(BlockTags.MINEABLE_WITH_HOE) -> "hoe"
            else -> "pickaxe"
        }
    }

    /**
     * Check if the player is allowed to veinmine the block
     * @return the veinmine action if the player is allowed to veinmine the block, null otherwise
     */
    fun allowedToVeinmine(world: Level, player: Player, pos: BlockPos, state: BlockState): VeinmineAction? {
        if (!active) return null

        // Check if player is in creative
        if (player.gameMode() == GameType.CREATIVE) return null

        val material = state.key()
        if (state.isAir) return null

        val settings = ConfigManager.settings
        if (settings.permissionRestricted && !Permissions.check(player, permissionVeinmine)) return null
        val hasClientBypass = settings.client.allBlocks && FabricNetworking.registeredPlayers.containsKey(player.uuid)
        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val isWhitelisted = isGroupBlock || ConfigManager.veinBlocks.contains(material)

        if (!isWhitelisted && !hasClientBypass) return null

        // Check for sneak config
        if (settings.mustSneak && !player.isCrouching) return null

        // Check for cooldown
        if (cooldown.contains(player.uuid)) return null

        // Check for correct tool (if block group tools are empty, it means all tools are allowed)
        val mainHandItem = player.mainHandItem
        if (settings.needCorrectTool && (state.requiresCorrectToolForDrops() && !mainHandItem.isCorrectToolForDrops(state))) return null
        if (!hasClientBypass && isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(mainHandItem.key())) return null

        // Check for enchantment if active
        if (enchantmentActive && !mainHandItem.enchantments.keySet().any { it.`is`(VEINMINE) }) return null

        // Gather veinmine action information
        val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
        return VeinmineAction(state, pos, blocks, mainHandItem, mutableSetOf(), player, world, pos, settings)
    }

    /**
     * Recursively break blocks around the source block until the vein stops
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

            // Only break if action is mining
            if (size != 0 && shouldBreak) {
                mcCoroutineTask(delay = (settings.delay * vBlock.distance).ticks) {
                    // Delay if necessary & check again if the block is still valid
                    if (settings.delay != 0) {
                        if (!targetTypes.contains(vBlock.block.key())) return@mcCoroutineTask
                    }

                    vBlock.block.destroyBlock(tool, world, pos, player, sourceLocation)
                    if (settings.decreaseDurability) damageItem(tool, player)
                }
            }
            processedBlocks.add(pos)

            // Process blocks around the current block
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

    /**
     * Used to extract the drop logic from [Block.dropResources] to allow for custom handling of drops
     */
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
            val dropPos = if (ConfigManager.settings.mergeItemDrops) initialSource else blockPos
            Block.popResource(world, dropPos, drop)
        }
        blockState.spawnAfterBreak(serverLevel, blockPos, tool, true)
    }

    private fun damageItem(item: ItemStack, player: Player) {
        if (item.isEmpty) return
        item.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
    }

    data class VeinmineAction(
        val currentBlock: BlockState,
        val currentPosition: BlockPos,
        val targetTypes: Set<ResourceLocation>,
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