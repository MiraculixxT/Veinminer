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
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

object VeinMinerEvent {
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

        // Check for correct tool (if block group tools are empty, it means all tools are allowed)
        val item = player.inventory.itemInMainHand
        if (settings.needCorrectTool && (block.getDrops(item).isEmpty() || item.isEmpty)) return null
        if (isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(item.type.key)) return null

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

    private fun Block.destroy(tool: ItemStack, drop: Boolean) {
        if (drop) breakNaturally(tool, true, true)
        else {
            val center = location.toCenterLocation()
            world.playSound(center, blockSoundGroup.breakSound, 1f, 1f)
            world.spawnParticle(Particle.BLOCK, center, 20, blockData)
            type = Material.AIR
        }
    }

    private class VeinminerEvent(block: Block, breaker: Player, val sourceLocation: Location) : BlockBreakEvent(block, breaker)

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
        val block: Block,
        val distance: Int
    )
}
