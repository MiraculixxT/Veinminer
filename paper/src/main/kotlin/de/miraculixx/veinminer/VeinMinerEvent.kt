package de.miraculixx.veinminer

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.config.ConfigManager
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
    private fun Material.groupedBlocks(): Set<Material> = ConfigManager.groups.filter { it.blocks.contains(this) }.map { it.blocks }.flatten().toSet()

    private val onBlockBreak = listen<BlockBreakEvent> {
        val player = it.player
        val material = it.block.type

        val settings = ConfigManager.settings
        if (settings.permissionRestricted && !player.hasPermission(permissionVeinmine)) return@listen
        val materialGroup = material.groupedBlocks()
        if (ConfigManager.veinBlocks.contains(material) || materialGroup.isNotEmpty()) {
            // Check for sneak config
            if (settings.mustSneak && !player.isSneaking) return@listen
            // Check for cooldown
            if (cooldown.contains(player.uniqueId)) return@listen
            // Check for correct tool
            if (settings.needCorrectTool && it.block.getDrops(it.player.inventory.itemInMainHand).isEmpty()) return@listen

            // Perform veinminer
            val item = player.inventory.itemInMainHand
            breakAdjusted(it.block, materialGroup + setOf(material), item, settings.delay, settings.maxChain, mutableSetOf(), player, settings.searchRadius)

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
    private fun breakAdjusted(source: Block, target: Set<Material>, item: ItemStack, delay: Int, max: Int, processedBlocks: MutableSet<Block>, player: Player, searchRadius: Int): Int {
        if (!target.contains(source.type) || processedBlocks.contains(source)) return 0
        val size = processedBlocks.size
        if (size >= max) return 0
        if (item.isEmpty) return 0
        if (size != 0) {
            source.breakNaturally(item, true, true)
            // TODO somehow grab the item and teleport it to the player (if setting is on)
            damageItem(item, 1, player)
        }

        processedBlocks.add(source)
        (-searchRadius..searchRadius).forEach { x ->
            (-searchRadius..searchRadius).forEach { y ->
                (-searchRadius..searchRadius).forEach z@{ z ->
                    if (x == 0 && y == 0 && z == 0) return@z
                    val block = source.world.getBlockAt(source.x + x, source.y + y, source.z + z)
                    if (delay == 0) breakAdjusted(block, target, item, delay, max, processedBlocks, player, searchRadius)
                    else taskRunLater(delay.toLong()) {
                        if (breakAdjusted(block, target, item, delay, max, processedBlocks, player, searchRadius) == 0) return@taskRunLater
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
