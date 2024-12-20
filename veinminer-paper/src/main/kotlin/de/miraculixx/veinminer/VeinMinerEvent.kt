package de.miraculixx.veinminer

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.Veinminer.Companion.VEINMINE
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.FixedBlockGroup
import de.miraculixx.veinminer.config.permissionVeinmine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()

    /**
     * @return a set of all blocks in the same group as this material. If the material is not in a group, it will return an empty set
     */
    private fun Material.groupedBlocks(): FixedBlockGroup<Material> {
        val blocks = mutableSetOf<Material>()
        val tools = mutableSetOf<Material>()
        ConfigManager.groups.forEach { if (it.blocks.contains(this)) { blocks.addAll(it.blocks); tools.addAll(it.tools) } }
        return FixedBlockGroup(blocks.toSet(), tools.toSet())
    }

    private val onBlockBreak = listen<BlockBreakEvent> {
        val player = it.player
        if (player.scoreboardTags.contains("veinminer")) {
            player.removeScoreboardTag("veinminer")
            println("Cancelled check")
            return@listen
        }

        val material = it.block.type
        if (it.isCancelled) return@listen

        val settings = ConfigManager.settings
        if (settings.permissionRestricted && !player.hasPermission(permissionVeinmine)) return@listen
        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val isWhitelisted = isGroupBlock || ConfigManager.veinBlocks.contains(material)

        if (isWhitelisted) {
            // Check for sneak config
            if (settings.mustSneak && !player.isSneaking) return@listen

            // Check for cooldown
            if (cooldown.contains(player.uniqueId)) return@listen

            // Check for correct tool (if block group tools are empty, it means all tools are allowed)
            val item = player.inventory.itemInMainHand
            if (settings.needCorrectTool && it.block.getDrops(item).isEmpty()) return@listen
            if (isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(item.type)) return@listen

            // Check for enchantment if active
            if (Veinminer.enchantmentActive && !item.enchantments.any { it.key.key == VEINMINE }) return@listen

            // Perform veinminer
            val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
            breakAdjusted(it.block, blocks, item, settings.delay, settings.maxChain, mutableSetOf(), player, settings.searchRadius, settings.decreaseDurability)

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
    }

    /**
     * Recursively break blocks around the source block until the vein stops
     * @return the number of blocks broken
     *
     * @param source the source block
     * @param target the set of blocks to break (all lists from the group)
     * @param item the item to break the blocks with
     * @param delay the delay between breaking blocks
     * @param max the maximum number of blocks to break
     */
    private fun breakAdjusted(
        source: Block,
        target: Set<Material>,
        item: ItemStack,
        delay: Int,
        max: Int,
        processedBlocks: MutableSet<Block>,
        player: Player,
        searchRadius: Int,
        damageItem: Boolean
    ): Int {
        if (!target.contains(source.type) || processedBlocks.contains(source)) return 0
        val size = processedBlocks.size
        if (size >= max) return 0
        if (item.isEmpty) return 0
        if (size != 0) {
            // Check if other plugins cancel the event
            player.addScoreboardTag("veinmine")
            if (!BlockBreakEvent(source, player).callEvent()) return 0
            source.breakNaturally(item, true, true)
            // TODO somehow grab the item and teleport it to the player (if setting is on)
            if (damageItem) damageItem(item, 1, player)
        }

        processedBlocks.add(source)
        (-searchRadius..searchRadius).forEach { x ->
            (-searchRadius..searchRadius).forEach { y ->
                (-searchRadius..searchRadius).forEach z@{ z ->
                    if (x == 0 && y == 0 && z == 0) return@z
                    val block = source.world.getBlockAt(source.x + x, source.y + y, source.z + z)
                    if (delay == 0) breakAdjusted(block, target, item, delay, max, processedBlocks, player, searchRadius, damageItem)
                    else taskRunLater(delay.toLong()) {
                        if (breakAdjusted(block, target, item, delay, max, processedBlocks, player, searchRadius, damageItem) == 0) return@taskRunLater
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
        if (item.type.maxDurability == 0.toShort() || item.isEmpty) return false
        return item.damage(amount, player).isEmpty
    }

    fun disable() {
        onBlockBreak.unregister()
    }
}
