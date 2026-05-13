package de.miraculixx.veinminer.utils

import net.minecraft.server.MinecraftServer

/**
 * Holds a reference to the MinecraftServer instance after launch.
 * Unused on Paper platforms.
 */
@Volatile
var mcServer: MinecraftServer? = null
