package de.miraculixx.veinminer.config

import net.kyori.adventure.text.format.TextColor
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText

inline fun <reified T> Path.load(default: T): T {
    return if (!exists()) {
        createParentDirectories()
        createFile()
        default
    } else {
        try {
            json.decodeFromString<T>(readText())
        } catch (e: Exception) {
            println("[Veinminer] Failed to load ${this.fileName} config: Reason: ${e.message}")
            default
        }
    }
}

fun Int.color() = TextColor.color(this)