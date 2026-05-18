package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.data.BlockPosition
import net.minecraft.resources.Identifier
import org.bukkit.Location
import org.bukkit.NamespacedKey

fun Location.toVeinminer() = BlockPosition(blockX, blockY, blockZ)
fun NamespacedKey.toVeinminer() = Identifier.fromNamespaceAndPath(namespace, key)
