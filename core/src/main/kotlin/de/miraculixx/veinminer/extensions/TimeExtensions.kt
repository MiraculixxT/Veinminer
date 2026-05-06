package de.miraculixx.veinminer.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
