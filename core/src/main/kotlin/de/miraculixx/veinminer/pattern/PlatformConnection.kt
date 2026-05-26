package de.miraculixx.veinminer.pattern

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.VeinminerSettings
import net.minecraft.resources.ResourceLocation

interface BlockAwareness {
    fun getBlockType(pos: BlockPosition): ResourceLocation
    fun isActionTarget(pos: BlockPosition): Boolean = true
    fun breakBlock(pos: BlockPosition, ticks: Int): Boolean
}

data class VeinmineAction<Tool, Player>(
    val currentBlock: BlockPosition,
    val targetTypes: Set<ResourceLocation>,
    val tool: Tool,
    val player: Player,
    val sourceLocation: BlockPosition,
    val settings: VeinminerSettings,
    val face: Surface
)
