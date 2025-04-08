package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.utils.json
import net.kyori.adventure.text.format.TextColor
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

inline fun <reified T> Path.load(default: T): T {
    return if (notExists()) {
        createParentDirectories()
        createFile()
        writeText(json.encodeToString(default))
        default
    } else {
        try {
            json.decodeFromString<T>(readText())
        } catch (_: Exception) { default }
    }
}

fun Int.color() = TextColor.color(this)