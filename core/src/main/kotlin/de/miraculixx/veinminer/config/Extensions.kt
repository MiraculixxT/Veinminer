package de.miraculixx.veinminer.config

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.format.TextColor
import java.io.File
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
        } catch (_: Exception) { default }
    }
}

fun Int.color() = TextColor.color(this)