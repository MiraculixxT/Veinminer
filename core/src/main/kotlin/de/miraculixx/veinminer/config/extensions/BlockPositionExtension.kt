package de.miraculixx.veinminer.config.extensions

import de.miraculixx.veinminer.config.data.BlockPosition
import net.minecraft.core.BlockPos

fun BlockPosition.toNMS() = BlockPos(x, y, z)
fun BlockPos.toVeinminer() = BlockPosition(x, y, z)
