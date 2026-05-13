@file:Suppress("UnstableApiUsage")

package de.miraculixx.veinminer

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.server
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.Veinminer.Companion.VEINMINE
import de.miraculixx.veinminer.config.PaperConfigManager
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.event.HighlightCache
import de.miraculixx.veinminer.utils.debug
import de.miraculixx.veinminer.utils.permissionVeinmine
import de.miraculixx.veinminer.network.NetworkRouter
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Paper implementation of the common veinmining logic.
 * Native implementation is prefered here over using the common NMS implementation.
 */
object VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()
    var enabled: Boolean = true
    private val attributeNamespace = NamespacedKey(Veinminer.INSTANCE, "veinmine_speed")

    /**
     * @return a set of all blocks in the same group as this material. If the material is not in a group, it will return an empty set
     */

    private fun NamespacedKey.groupedBlocks(): FixedBlockGroup<NamespacedKey> {
        val blocks = mutableSetOf<NamespacedKey>()
        val tools = mutableSetOf<NamespacedKey>()
        var override: VeinminerSettingsOverride? = null

        PaperConfigManager.groups.forEach {
            if (it.blocks.contains(this)) {
                if (override == null) { // Capture first override. This behavior may need improvement
                    override = it.override
                }
                blocks.addAll(it.blocks)
                tools.addAll(it.tools)
            }
        }

        return FixedBlockGroup(blocks.toSet(), tools.toSet(), override)
    }

    /**
     * Used to modify the block breaking speed when veinmining.
     * Only used when multi setting is over 0.0.
     */
    @Suppress("unused")
    private val onBlockDamage = listen<BlockDamageEvent> {
        if (it.isCancelled || it.instaBreak || !enabled) return@listen
        val player = it.player
        val veinmineInfo = allowedToVeinmine(player, it.block) ?: return@listen
        val multiplicator = veinmineInfo.settings.miningSpeedModifier
        if (multiplicator <= 0.0) return@listen

        val amount = veinmineInfo.veinmine(false)
        if (amount <= 1) return@listen

        val speed = veinmineInfo.settings.calculateBreakSpeedModifier(amount, multiplicator)
        val modifier = AttributeModifier(attributeNamespace, speed, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
        val attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return@listen
        attribute.removeModifier(attributeNamespace)
        attribute.addTransientModifier(modifier)
        if (debug) player.sendMessage(cmp("Modifier: ${"%.3f".format(speed + 1.0)}x for $amount blocks"))
    }

    @Suppress("unused")
    private val onBlockDamageAbort = listen<BlockDamageAbortEvent> {
        it.player.removeAttribute()
    }

    @Suppress("unused")
    private val onBlockBreak = listen<BlockBreakEvent>(priority = EventPriority.HIGH) {
        it.player.removeAttribute() // Always remove
        if (it.isCancelled || !enabled) return@listen

        val player = it.player
        val block = it.block

        // Check if the event is triggered by Veinminer
        if (it is VeinminerEvent) {
            if (!it.isDropItems) return@listen
            var settings = PaperConfigManager.settings

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
            if (dropItemEvent.exp > 0) block.world.spawn(location, ExperienceOrb::class.java).experience =
                dropItemEvent.exp

            it.isDropItems = false
            it.expToDrop = 0
            return@listen
        }

        HighlightCache.invalidate(block.world, BlockPosition(block.x, block.y, block.z))
        val veinmineInfo = allowedToVeinmine(player, block) ?: return@listen
        it.isCancelled = true // Cancel the original event

        val amount = veinmineInfo.veinmine(true)
        player.incrementStatistic(Statistic.MINE_BLOCK, block.type, amount) // Not -1 because we cancel the event

        // Check for cooldown config
        val cooldownTime = veinmineInfo.settings.cooldown
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
        if (debug) Veinminer.LOGGER.info("Checking if ${player.name} is allowed to veinmine ${block.type.key}")

        // Check if player is in creative
        if (player.gameMode == GameMode.CREATIVE) return null

        val uuid = player.uniqueId
        val hasClient = NetworkRouter.registeredPlayers.contains(uuid)
        val material = block.type.key

        // Check if player has the client mod and pressed the key
        if (hasClient && !NetworkRouter.readyToVeinmine.contains(uuid)) return null

        // Gather correct settings layer
        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val settings = PaperConfigManager.settings.applyOverrides(hasClient, blockGroup.override)

        if (settings.permissionRestricted && !player.hasPermission(permissionVeinmine)) return null
        val hasClientBypass = settings.client.allBlocks && NetworkRouter.registeredPlayers.containsKey(player.uniqueId)
        val isWhitelisted = isGroupBlock || PaperConfigManager.veinBlocks.contains(material)
        if (debug) Veinminer.LOGGER.info(" - Group: $blockGroup, Global: ${PaperConfigManager.veinBlocks}, isWhitelisted: $isWhitelisted")

        // Check if client is required
        if (settings.client.require && !hasClient) return null

        // Check if the block is allowed at all
        if (!isWhitelisted && !hasClientBypass) return null

        // Check for sneak config
        if (settings.mustSneak && !player.isSneaking) return null

        // Check for cooldown
        if (cooldown.contains(player.uniqueId)) return null

        // Check for correct tool (if block group tools are empty, it means all tools are allowed)
        val item = player.inventory.itemInMainHand
        if (debug) Veinminer.LOGGER.info(" - Tool: ${item.type.key}")
        if (settings.needCorrectTool && (block.getDrops(item).isEmpty() || item.isEmpty)) return null
        if (isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(item.type.key)) return null
        // Fall back to vanilla break on last durability point so one normal block can still be mined.
        if (settings.decreaseDurability && item.remainingDurability() <= 1) return null

        // Check for enchantment if active
        if (Veinminer.enchantmentActive && !item.enchantments.any { it.key.key == VEINMINE }) return null

        // Perform veinminer
        val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
        if (debug) Veinminer.LOGGER.info(" - Allowed with $blocks")
        return VeinmineAction(block, blocks, item, mutableSetOf(), player, block.location.toCenterLocation(), settings)
    }

    /**
     * Recursively break blocks around the source block until the vein stops
     * @return the number of blocks broken
     */
    fun VeinmineAction.veinmine(shouldBreak: Boolean): Int {
        val targets = targetTypes
        val visited = processedBlocks
        if (!targets.contains(currentBlock.type.key)) return 0

        val maxChain = settings.maxChain
        val searchRadius = settings.searchRadius
        val decreaseDurability = settings.decreaseDurability
        val needCorrectTool = settings.needCorrectTool
        val delay = settings.delay
        val runsAsync = VeinminerCompatibility.runsAsync

        val queue = ArrayDeque<VeinmineBlock>()
        queue.add(VeinmineBlock(currentBlock, 0))

        while (queue.isNotEmpty()) {
            val vBlock = queue.removeFirst()
            val block = vBlock.block
            if (visited.contains(block)) continue
            if (visited.size >= maxChain) break

            if (shouldBreak) {
                if (needCorrectTool && tool.isEmpty) continue
                if (decreaseDurability && tool.remainingDurability() <= 1) continue

                val tickDelay = (delay * vBlock.distance).toLong()
                if (runsAsync) {
                    if (tickDelay == 0L) {
                        server.regionScheduler.execute(Veinminer.INSTANCE, block.location) {
                            triggerBreaking(block)
                        }
                    } else {
                        server.regionScheduler.runDelayed(Veinminer.INSTANCE, block.location, {
                            triggerBreaking(block)
                        }, tickDelay)
                    }
                } else {
                    taskRunLater(tickDelay, true) {
                        triggerBreaking(block)
                    }
                }
            }
            visited.add(block)

            val nextDist = vBlock.distance + 1
            val world = block.world
            val bx = block.x; val by = block.y; val bz = block.z
            for (x in -searchRadius..searchRadius) {
                for (y in -searchRadius..searchRadius) {
                    for (z in -searchRadius..searchRadius) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val newBlock = world.getBlockAt(bx + x, by + y, bz + z)
                        if (visited.contains(newBlock)) continue
                        if (!targets.contains(newBlock.type.key)) continue
                        queue.add(VeinmineBlock(newBlock, nextDist))
                    }
                }
            }
        }
        return visited.size
    }

    private fun VeinmineAction.triggerBreaking(block: Block) {
        // Delay if necessary & check again if the block is still valid
        if (!VeinminerCompatibility.runsAsync) {
            if (settings.delay != 0) {
                if (!targetTypes.contains(block.type.key)) return
            }
        }
        // Re-check remaining durability at execution time to prevent queued tasks from breaking the tool.
        if (settings.decreaseDurability && tool.remainingDurability() <= 1) return

        // Check if other plugins cancel the event
        val veinminerEvent = VeinminerEvent(block, player, sourceLocation, block.getXP(tool))
        if (!veinminerEvent.callEvent()) return
        block.destroy()
        if (settings.decreaseDurability) damageItem(tool, 1, player)
    }

    /**
     * @return true if the item was broken
     */
    @Suppress("SameParameterValue")
    private fun damageItem(item: ItemStack, amount: Int, player: Player): Boolean {
        if (item.isEmpty) return false
        if (item.type.maxDurability == 0.toShort()) return false
        return item.damage(amount, player).isEmpty
    }

    private fun ItemStack.remainingDurability(): Int {
        if (isEmpty) return 0
        val maxDurability = type.maxDurability.toInt()
        if (maxDurability <= 0) return Int.MAX_VALUE
        return maxDurability - (getData(DataComponentTypes.DAMAGE) ?: 0)
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
        val position = craftBlock.position
        val nmsState = craftBlock.level.getBlockState(position)
        val nmsItem = (tool as CraftItemStack).handle ?: net.minecraft.world.item.ItemStack.EMPTY
        return nmsState.block.getExpDrop(nmsState, craftBlock.craftWorld.handle, position, nmsItem, true)
    }

    private fun Player.removeAttribute() {
        getAttribute(Attribute.BLOCK_BREAK_SPEED)?.removeModifier(attributeNamespace)
    }

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
        block: Block, val blockState: BlockState, val player: Player, var items: MutableList<ItemStack>, var exp: Int
    ) : BlockExpEvent(block, exp)

    data class VeinmineAction(
        val currentBlock: Block,
        val targetTypes: Set<NamespacedKey>,
        val tool: ItemStack,
        val processedBlocks: MutableSet<Block>,
        val player: Player,
        val sourceLocation: Location,
        val settings: VeinminerSettings
    )

    data class VeinmineBlock(
        val block: Block, val distance: Int
    )
}
