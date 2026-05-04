package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.config.data.BlockPosition
import de.miraculixx.veinminer.config.pattern.Surface
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

fun BlockPosition.toNMS() = BlockPos(x, y, z)
fun BlockPos.toVeinminer() = BlockPosition(x, y, z)

fun Direction.toVeinminer() = when (this) {
    Direction.DOWN -> Surface.DOWN
    Direction.UP -> Surface.UP
    Direction.NORTH -> Surface.NORTH
    Direction.SOUTH -> Surface.SOUTH
    Direction.WEST -> Surface.WEST
    Direction.EAST -> Surface.EAST
}

fun Surface.toNMS() = when (this) {
    Surface.DOWN -> Direction.DOWN
    Surface.UP -> Direction.UP
    Surface.NORTH -> Direction.NORTH
    Surface.SOUTH -> Direction.SOUTH
    Surface.WEST -> Direction.WEST
    Surface.EAST -> Direction.EAST
}