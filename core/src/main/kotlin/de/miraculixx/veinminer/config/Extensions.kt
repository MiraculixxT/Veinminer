package de.miraculixx.veinminer.config

import net.kyori.adventure.text.format.TextColor
import java.io.File

inline fun <reified T> File.loadFile(default: T): T {
    return if (!exists()) {
        parentFile.mkdirs()
        createNewFile()
        default
    } else {
        try {
            json.decodeFromString<T>(readText())
        } catch (_: Exception) { default }
    }
}

fun Int.color() = TextColor.color(this)