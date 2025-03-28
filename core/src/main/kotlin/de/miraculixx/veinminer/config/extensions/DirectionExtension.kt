package de.miraculixx.veinminer.config.extensions

import de.miraculixx.veinminer.config.pattern.Surface
import net.minecraft.core.Direction

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