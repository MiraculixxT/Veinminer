package de.miraculixx.veinminer.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val Int.ticks
    get() = (this * 50).milliseconds

fun mcCoroutineAsync(time: Duration, block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        delay(time)
        block()
    }
}

fun mcCoroutineSync(mcServer: MinecraftServer, ticks: Int, block: () -> Unit) {
    if (ticks <= 0) {
        mcServer.execute(block)
        return
    }
    CoroutineScope(Dispatchers.Default).launch {
        delay(ticks.ticks)
        mcServer.execute(block)
    }
}
