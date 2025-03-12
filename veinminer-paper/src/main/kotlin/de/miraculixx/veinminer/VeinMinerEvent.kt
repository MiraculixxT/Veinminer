package de.miraculixx.veinminer

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.veinminer.Veinminer.Companion.VEINMINE
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.config.data.FixedBlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.utils.permissionVeinmine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.entity.ExperienceOrb
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
        if (it.isCancelled) return@listen

        val player = it.player
        val settings = ConfigManager.settings
        val block = it.block

        // Check if the event is triggered by Veinminer
        if (it is VeinminerEvent) {
//            println("Veinminer Event: ${it.sourceLocation}")

            // Check if item drops should be merged
            if (!settings.mergeItemDrops || !it.isDropItems) return@listen

            block.getDrops(player.inventory.itemInMainHand, player).forEach { drop ->
                block.world.dropItem(it.sourceLocation, drop)
            }
            if (it.expToDrop > 0) block.world.spawn(it.sourceLocation, ExperienceOrb::class.java).experience = it.expToDrop

            it.isDropItems = false
            it.expToDrop = 0
            return@listen
        }

        val material = block.type

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
            if (settings.needCorrectTool && (it.block.getDrops(item).isEmpty() || item.isEmpty)) return@listen
            if (isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(item.type)) return@listen

            // Check for enchantment if active
            if (Veinminer.enchantmentActive && !item.enchantments.any { it.key.key == VEINMINE }) return@listen

            // Perform veinminer
            val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
            VeinmineAction(it.block, blocks, item, mutableSetOf(), player, it.block.location.toCenterLocation(), settings)
                .breakAdjusted()
            it.isCancelled = true // Cancel the original event

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
     */
    private fun VeinmineAction.breakAdjusted(): Int {
        if (!targetTypes.contains(currentBlock.type) || processedBlocks.contains(currentBlock)) return 0
        val size = processedBlocks.size
        if (size >= settings.maxChain) return 0
        if (settings.needCorrectTool && tool.isEmpty) return 0

        // Check if other plugins cancel the event
        if (!VeinminerEvent(currentBlock, player, sourceLocation).callEvent()) return 0
        currentBlock.destroy(tool, !settings.mergeItemDrops)
        if (settings.decreaseDurability) damageItem(tool, 1, player)

        processedBlocks.add(currentBlock)
        val searchRadius = settings.searchRadius
        (-searchRadius..searchRadius).forEach { x ->
            (-searchRadius..searchRadius).forEach { y ->
                (-searchRadius..searchRadius).forEach z@{ z ->
                    if (x == 0 && y == 0 && z == 0) return@z
                    val block = currentBlock.world.getBlockAt(currentBlock.x + x, currentBlock.y + y, currentBlock.z + z)
                    if (settings.delay == 0) copy(currentBlock = block).breakAdjusted()
                    else taskRunLater(settings.delay.toLong()) {
                        if (copy(currentBlock = block).breakAdjusted() == 0) return@taskRunLater
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

    private fun Block.destroy(tool: ItemStack, drop: Boolean) {
        if (drop) breakNaturally(tool, true, true)
        else {
            val center = location.toCenterLocation()
            world.playSound(center, blockSoundGroup.breakSound, 1f, 1f)
            world.spawnParticle(Particle.BLOCK, center, 20, blockData)
            type = Material.AIR
        }
    }

    fun disable() {
        onBlockBreak.unregister()
    }

    private class VeinminerEvent(block: Block, breaker: Player, val sourceLocation: Location): BlockBreakEvent(block, breaker)

    private data class VeinmineAction(
        val currentBlock: Block,
        val targetTypes: Set<Material>,
        val tool: ItemStack,
        val processedBlocks: MutableSet<Block>,
        val player: Player,
        val sourceLocation: Location,
        val settings: VeinminerSettings
    )
}
