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
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.utils.debug
import de.miraculixx.veinminer.utils.permissionVeinmine
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.pattern.BlockAwareness
import de.miraculixx.veinminer.pattern.VeinmineAction
import de.miraculixx.veinminer.pattern.Veinmining
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.resources.Identifier
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

    fun allowedToVeinmine(player: Player, block: Block): VeinmineAction? {
        if (debug) Veinminer.LOGGER.info("Checking if ${player.name} is allowed to veinmine ${block.type.key}")

        // Check if player is in creative
        if (player.gameMode == GameMode.CREATIVE) return null

        val uuid = player.uniqueId
        val hasClient = NetworkRouter.registeredPlayers.contains(uuid)
        val material = block.type.key

        // Check if player has the client mod and pressed the key
        if (hasClient && !NetworkRouter.isReady(uuid)) return null

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
        val face = NetworkRouter.lastSurface[player.uniqueId] ?: Surface.UP
        val currentPos = block.location.toVeinminer()
        val blocksNMS = blocks.map { it.toVeinminer() }.toSet()
        return VeinmineAction(currentPos, blocksNMS, item, mutableSetOf(), player, currentPos, settings, face)
    }

    /**
     * Recursively break blocks around the source block until the vein stops
     * @return the number of blocks broken
     */
    fun VeinmineAction.veinmine(shouldBreak: Boolean): Int {
        val iTool = tool as ItemStack
        val iPlayer = player as Player
        val world = iPlayer.world

        if (iTool.isEmpty) return 0
        val shape = NetworkRouter.activeShape(iPlayer.uniqueId) ?: Shape.NORMAL
        val maxDepth = NetworkRouter.maxDepth(iPlayer.uniqueId)
        val delay = settings.delay

        val blockAwareness = object : BlockAwareness {
            override fun getBlockType(pos: BlockPosition): Identifier {
                return world.getBlockAt(pos.x, pos.y, pos.z).type.key.toVeinminer()
            }

            override fun breakBlock(pos: BlockPosition, ticks: Int): Boolean {
                if (!shouldBreak) return false // safeguard
                if ((tool as ItemStack).remainingDurability() <= 1) return false // tool "broken"
                val state = world.getBlockAt(pos.x, pos.y, pos.z)
                scheduleBreak(state, delay.toLong())
                return true
            }
        }

        val hits = Veinmining.veinmine(this, blockAwareness, shape, maxDepth, shouldBreak)
        return hits.size
    }

    private fun VeinmineAction.scheduleBreak(block: Block, delay: Long) {
        if (VeinminerCompatibility.runsAsync) {
            if (delay == 0L) {
                server.regionScheduler.execute(Veinminer.INSTANCE, block.location) { triggerBreaking(block) }
            } else {
                server.regionScheduler.runDelayed(Veinminer.INSTANCE, block.location, { triggerBreaking(block) }, delay)
            }
        } else {
            taskRunLater(delay, true) { triggerBreaking(block) }
        }
    }

    private fun VeinmineAction.triggerBreaking(block: Block) {
        // Delay if necessary & check again if the block is still valid
        if (!VeinminerCompatibility.runsAsync) {
            if (settings.delay != 0) {
                if (!targetTypes.contains(block.type.key.toVeinminer())) return
            }
        }
        // Re-check remaining durability at execution time to prevent queued tasks from breaking the tool
        val iTool = tool as? ItemStack ?: return
        if (settings.decreaseDurability && iTool.remainingDurability() <= 1) return

        // Check if other plugins cancel the event
        val iPlayer = player as? Player ?: return
        val sourceLoc = Location(iPlayer.world, sourceLocation.x.toDouble(), sourceLocation.y.toDouble(), sourceLocation.z.toDouble())
        val veinminerEvent = VeinminerEvent(block, iPlayer, sourceLoc, block.getXP(iTool))
        if (!veinminerEvent.callEvent()) return
        block.destroy()
        if (settings.decreaseDurability) damageItem(iTool, 1, iPlayer)
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

    private fun Location.toVeinminer() = BlockPosition(blockX, blockY, blockZ)
    private fun NamespacedKey.toVeinminer() = Identifier.fromNamespaceAndPath(namespace, key)

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
}
