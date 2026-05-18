package de.miraculixx.veinminer.pattern

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.VeinminerSettings
import net.minecraft.resources.Identifier

interface BlockAwareness {
    fun getBlockType(pos: BlockPosition): Identifier
    fun breakBlock(pos: BlockPosition, ticks: Int): Boolean
}

data class VeinmineAction(
    val currentBlock: BlockPosition,
    val targetTypes: Set<Identifier>,
    val tool: Any, // ItemStack, platform dependent
    val processedBlocks: MutableSet<BlockPosition>,
    val player: Any, // Player/ServerPlayer, platform dependent
    val sourceLocation: BlockPosition,
    val settings: VeinminerSettings,
    val face: Surface
)
