package de.miraculixx.veinminer

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.config.ConfigManager
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import java.util.*

class VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()

    private val onBlockBreak = listen<BlockBreakEvent> {
        val player = it.player
        val material = it.block.type

        val settings = ConfigManager.settings
        if (ConfigManager.veinBlocks.contains(material)) {
            // Check for sneak config
            if (settings.mustSneak && !player.isSneaking) return@listen
            // Check for cooldown
            if (cooldown.contains(player.uniqueId)) return@listen
            // Check for correct tool
            if (settings.needCorrectTool && !it.isDropItems) return@listen

            // Perform veinminer
            val item = player.inventory.itemInMainHand
            val blocks = breakAdjusted(it.block, material, item, settings.delay, settings.maxChain, mutableSetOf())
            if (blocks > 1) damageItem(item, blocks - 1)

            // Check for cooldown config
            val cooldownTime = settings.cooldown
            if (cooldownTime > 0) {
                cooldown.add(player.uniqueId)
                taskRunLater(cooldownTime.toLong()) {
                    cooldown.remove(player.uniqueId)
                }
            }
        }
    }

    /**
     * Recursively break blocks around the source block until vein stops
     * @return the number of blocks broken
     */
    private fun breakAdjusted(source: Block, target: Material, item: ItemStack, delay: Int, max: Int, processedBlocks: MutableSet<Block>): Int {
        if (source.type != target || processedBlocks.contains(source)) return 0
        val size = processedBlocks.size
        if (size >= max) return 0
        if (size != 0) source.breakNaturally(item, true, true)
        processedBlocks.add(source)
        (-1..1).forEach { x ->
            (-1..1).forEach { y ->
                (-1..1).forEach z@{ z ->
                    if (x == 0 && y == 0 && z == 0) return@z
                    val block = source.world.getBlockAt(source.x + x, source.y + y, source.z + z)
                    if (delay == 0) breakAdjusted(block, target, item, delay, max, processedBlocks)
                    else taskRunLater(delay.toLong()) {
                        if (breakAdjusted(block, target, item, delay, max, processedBlocks) == 0) return@taskRunLater
                        damageItem(item, 1)
                    }
                }
            }
        }
        return processedBlocks.size
    }

    private fun damageItem(item: ItemStack, amount: Int) {
        if (item.type.maxDurability == 0.toShort()) return
        item.editMeta(Damageable::class.java) { meta ->
            meta.damage = (meta.damage + amount).coerceAtLeast(0)
        }
    }

    fun disable() {
        onBlockBreak.unregister()
    }
}