package de.miraculixx.veinminer

import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import java.util.*
import java.util.logging.Logger


class Veinminer : ModInitializer {
    companion object {
        const val MOD_ID = "veinminer"
        lateinit var INSTANCE: ModContainer
        var active = true
    }

    private lateinit var fabricLoader: FabricLoader

    private val cooldown = mutableSetOf<UUID>()

    override fun onInitialize() {
        fabricLoader = FabricLoader.getInstance()
        INSTANCE = fabricLoader.getModContainer(MOD_ID).get()
        LOGGER.info("Veinminer Version: ${INSTANCE.metadata.version} (fabric)")

        VeinminerCommand

        PlayerBlockBreakEvents.BEFORE.register { world, player, pos, state, _ ->
            if (!active) return@register true
            val material = state.block.descriptionId

            val settings = ConfigManager.settings
            if (ConfigManager.veinBlocks.contains(material)) {
                // Check for sneak config
                if (settings.mustSneak && !player.isCrouching) return@register true
                // Check for cooldown
                if (cooldown.contains(player.uuid)) return@register true

                // Check for correct tool
                val mainHandItem = player.mainHandItem
                if (settings.needCorrectTool && (state.requiresCorrectToolForDrops() && !mainHandItem.isCorrectToolForDrops(state))) return@register true

                // Perform veinminer
                breakAdjusted(state, material, mainHandItem, settings.delay, settings.maxChain, mutableSetOf(), world, pos, player)

                // Check for cooldown config
                val cooldownTime = settings.cooldown
                if (cooldownTime > 0) {
                    cooldown.add(player.uuid)

                    mcCoroutineTask(delay = cooldownTime.ticks) {
                        cooldown.remove(player.uuid)
                    }
                }
            }
            return@register true
        }
    }

    /**
     * Recursively break blocks around the source block until vein stops
     * @return the number of blocks broken
     */
    private fun breakAdjusted(
        source: BlockState,
        target: String,
        item: ItemStack,
        delay: Int,
        max: Int,
        processedBlocks: MutableSet<BlockPos>,
        world: Level,
        position: BlockPos,
        player: Player
    ): Int {
        if (source.block.descriptionId != target || processedBlocks.contains(position)) return 0
        val size = processedBlocks.size
        if (size >= max) return 0
        if (item.isEmpty) return 0
        if (size != 0) {
            source.destroyBlock(item, world, position, player)
            damageItem(item, player)
        }
        processedBlocks.add(position)
        (-1..1).forEach { x ->
            (-1..1).forEach { y ->
                (-1..1).forEach z@{ z ->
                    if (x == 0 && y == 0 && z == 0) return@z
                    val newPos = BlockPos(position.x + x, position.y + y, position.z + z)
                    val block = world.getBlockState(newPos)
                    if (delay == 0) breakAdjusted(block, target, item, delay, max, processedBlocks, world, newPos, player)
                    else mcCoroutineTask(delay = delay.ticks) {
                        if (breakAdjusted(block, target, item, delay, max, processedBlocks, world, newPos, player) == 0) return@mcCoroutineTask
                    }
                }
            }
        }
        return processedBlocks.size
    }

    private fun BlockState.destroyBlock(item: ItemStack, world: Level, position: BlockPos, player: Player) {
        val block = block
        if (block !== Blocks.AIR && (!requiresCorrectToolForDrops() || item.isCorrectToolForDrops(this))) {
            Block.dropResources(this, world, position, world.getBlockEntity(position), player, item)

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

    private fun damageItem(item: ItemStack, player: Player) {
        item.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
    }
}

val LOGGER: Logger = Logger.getLogger(Veinminer.MOD_ID)