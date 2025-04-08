package de.miraculixx.veinminer

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.Veinminer.Companion.VEINMINE
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.FixedBlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.utils.permissionVeinmine
import de.miraculixx.veinminer.networking.PaperNetworking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

// 1.20.5+ only
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.inventory.CraftItemStack


class VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()
    var enabled: Boolean = true

    /**
     * @return a set of all blocks in the same group as this material. If the material is not in a group, it will return an empty set
     */

    private fun NamespacedKey.groupedBlocks(): FixedBlockGroup<NamespacedKey> {
        val blocks = mutableSetOf<NamespacedKey>()
        val tools = mutableSetOf<NamespacedKey>()

        ConfigManager.groups.forEach {
            if (it.blocks.contains(this)) {
                blocks.addAll(it.blocks)
                tools.addAll(it.tools)
            }
        }

        return FixedBlockGroup(blocks.toSet(), tools.toSet())
    }

    private val onBlockBreak = listen<BlockBreakEvent> {
        if (it.isCancelled || !enabled) return@listen

        val player = it.player
        val settings = ConfigManager.settings
        val block = it.block

        // Check if the event is triggered by Veinminer
        if (it is VeinminerEvent) {
            if (!it.isDropItems) return@listen

            // Invoke VeinminerDropEvent - allows other plugins to modify the items and exp dropped by Veinminer itself
            // Veinminer will drop the items that are still in the list and the remaining amount of experience
            val tool = player.inventory.itemInMainHand
            val drops = block.getDrops(tool, player).toMutableList<ItemStack>()
            val dropItemEvent = VeinminerDropEvent(block, block.state, player, drops, it.expToDrop).also(Event::callEvent)

            // Use source location as drop pos if setting is enabled
            val location = if (settings.mergeItemDrops) it.sourceLocation else block.location.toCenterLocation()

            drops.forEach { drop ->
                block.world.dropItem(location, drop)
            }
            if (dropItemEvent.exp > 0)
                block.world.spawn(location, ExperienceOrb::class.java).experience = dropItemEvent.exp

            it.isDropItems = false
            it.expToDrop = 0
            return@listen
        }

        // Check if player has the client mod and pressed the key
        val uuid = player.uniqueId
        if (PaperNetworking.registeredPlayers.contains(uuid) && !PaperNetworking.readyToVeinmine.contains(uuid)) return@listen

        val veinmineInfo = allowedToVeinmine(player, block) ?: return@listen
        it.isCancelled = true // Cancel the original event

        veinmineInfo.veinmine(true)

        // Check for cooldown config
        val cooldownTime = settings.cooldown
        if (cooldownTime > 0) {
            cooldown.add(player.uniqueId)

            CoroutineScope(Dispatchers.Default).launch {
                delay((cooldownTime * 50).milliseconds)
                cooldown.remove(player.uniqueId)
            }
        }
    }

    fun getPreferredToolIcon(type: Material): String {
        return when {
            Tag.MINEABLE_AXE.isTagged(type) -> "axe"
            Tag.MINEABLE_SHOVEL.isTagged(type) -> "shovel"
            Tag.MINEABLE_HOE.isTagged(type) -> "hoe"
            else -> "pickaxe"
        }
    }

    fun allowedToVeinmine(player: Player, block: Block): VeinmineAction? {
        val material = block.type.key

        val settings = ConfigManager.settings
        if (settings.permissionRestricted && !player.hasPermission(permissionVeinmine)) return null
        val hasClientBypass = settings.client.allBlocks && PaperNetworking.registeredPlayers.containsKey(player.uniqueId)
        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val isWhitelisted = isGroupBlock || ConfigManager.veinBlocks.contains(material)

        if (!isWhitelisted && !hasClientBypass) return null


        // Check for sneak config
        if (settings.mustSneak && !player.isSneaking) return null

        // Check for cooldown
        if (cooldown.contains(player.uniqueId)) return null

        // Perform veinminer
        val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
        VeinmineAction(
            it.block,
            blocks,
            item,
            mutableSetOf(),
            player,
            it.block.location.toCenterLocation(),
            settings
        ).breakAdjusted()
        it.isCancelled = true // Cancel the original event

        // Check for enchantment if active
        if (Veinminer.enchantmentActive && !item.enchantments.any { it.key.key == VEINMINE }) return null

        // Perform veinminer
        val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
        return VeinmineAction(block, blocks, item, mutableSetOf(), player, block.location.toCenterLocation(), settings)
    }

    /**
     * Recursively break blocks around the source block until the vein stops
     * @return the number of blocks broken
     */
    fun VeinmineAction.veinmine(shouldBreak: Boolean): Int {
        val queue: Queue<VeinmineBlock> = LinkedList()
        queue.add(VeinmineBlock(currentBlock, 0))

        while (queue.isNotEmpty()) {
            val vBlock = queue.poll()
            val block = vBlock.block
            if (!targetTypes.contains(block.type.key) || processedBlocks.contains(block)) continue
            val size = processedBlocks.size
            if (size >= settings.maxChain) continue
            if (settings.needCorrectTool && tool.isEmpty) continue

            // Only break if action is mining
            if (shouldBreak) {
                taskRunLater((settings.delay * vBlock.distance).toLong(), !VeinminerCompatibility.runsAsync) {
                    // Delay if necessary & check again if the block is still valid
                    if (settings.delay != 0) {
                        if (!targetTypes.contains(block.type.key)) return@taskRunLater
                    }

                    // Check if other plugins cancel the event
                    if (!VeinminerEvent(block, player, sourceLocation).callEvent()) return@taskRunLater
                    block.destroy(tool, !settings.mergeItemDrops)
                    if (settings.decreaseDurability) damageItem(tool, 1, player)
                }
            }
            processedBlocks.add(block)

            // Process blocks around the current block
            val searchRadius = settings.searchRadius
            (-searchRadius..searchRadius).forEach { x ->
                (-searchRadius..searchRadius).forEach { y ->
                    (-searchRadius..searchRadius).forEach z@{ z ->
                        if (x == 0 && y == 0 && z == 0) return@z
                        val newBlock = block.world.getBlockAt(block.x + x, block.y + y, block.z + z)

                        queue.add(VeinmineBlock(newBlock, vBlock.distance + 1))
                    }
                }
            }
        }
        return processedBlocks.size
    }

    /**
     * @return true if the item was broken
     */
    @Suppress("SameParameterValue")
    private fun damageItem(item: ItemStack, amount: Int, player: Player): Boolean {
        if (item.isEmpty) return false
        if (item.type.maxDurability == 0.toShort() || item.isEmpty) return false
        return item.damage(amount, player).isEmpty
    }

    private fun Block.destroy() {
        val center = location.toCenterLocation()
        world.playSound(center, blockSoundGroup.breakSound, 1f, 1f)
        world.spawnParticle(Particle.BLOCK, center, 20, blockData)
        type = Material.AIR
    }

    // 1.20.5+ only
    private fun Block.getXP(tool: ItemStack): Int {
        val craftBlock = this as CraftBlock
        val nmsState = craftBlock.nms
        val nmsItem = (tool as CraftItemStack).handle
        return nmsState.block.getExpDrop(nmsState, craftBlock.handle.minecraftWorld, craftBlock.position, nmsItem, true)
    }

    private class VeinminerEvent(block: Block, breaker: Player, val sourceLocation: Location) : BlockBreakEvent(block, breaker)

    /**
     * Custom block break event that is only triggered by Veinminer before each block break attempt.
     * Cancelling this event will prevent Veinminer from breaking the block and continuing in this direction.
     *
     * @param sourceLocation the location where the vein started
     */
    class VeinminerEvent(
        block: Block,
        breaker: Player,
        val sourceLocation: Location,
        exp: Int,
    ) : BlockBreakEvent(block, breaker) {
        init {
            expToDrop = exp
        }
    }

    /**
     * Custom block break event that is only triggered by Veinminer after a block has been broken.
     * Modify this event to change the items or experience dropped, changing anything else or cancelling the event will have no effect.
     *
     * @param block the block that was broken
     * @param blockState the state of the block that was broken
     * @param player the player that broke the block
     * @param items the items that will be dropped
     * @param exp the amount of experience that will be dropped
     */
    class VeinminerDropEvent(
        block: Block,
        val blockState: BlockState,
        val player: Player,
        var items: MutableList<ItemStack>,
        var exp: Int
    ) : BlockExpEvent(block, exp)

    private data class VeinmineAction(
        val currentBlock: Block,
        val targetTypes: Set<NamespacedKey>,
        val tool: ItemStack,
        val processedBlocks: MutableSet<Block>,
        val player: Player,
        val sourceLocation: Location,
        val settings: VeinminerSettings
    )

    data class VeinmineBlock(
        val block: Block,
        val distance: Int
    )
}
