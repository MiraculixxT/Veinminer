package de.miraculixx.veinminer.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import net.minecraft.server.TickTask
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val Int.ticks
    get() = (this * 50).milliseconds

fun mcCoroutineDelay(time: Duration, block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        delay(time)
        block()
    }
}

fun mcScheduleDelay(mcServer: MinecraftServer, ticks: Int, block: () -> Unit) {
    mcServer.schedule(TickTask(ticks) {
        block()
    })
}
