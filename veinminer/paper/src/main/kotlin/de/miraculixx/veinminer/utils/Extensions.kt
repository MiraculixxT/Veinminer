package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.data.BlockPosition
import net.minecraft.resources.ResourceLocation
import org.bukkit.Location
import org.bukkit.NamespacedKey

fun Location.toVeinminer() = BlockPosition(blockX, blockY, blockZ)
fun NamespacedKey.toVeinminer() = ResourceLocation.fromNamespaceAndPath(namespace, key)
