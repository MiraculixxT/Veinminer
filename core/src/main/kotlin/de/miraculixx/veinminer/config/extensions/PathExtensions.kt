package de.miraculixx.veinminer.config.extensions

import de.miraculixx.veinminer.config.utils.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.format.TextColor
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

inline fun <reified T> Path.load(default: T, instance: Json = json): T {
    return if (!exists()) {
        createParentDirectories()
        createFile()
        writeText(instance.encodeToString(default))
        println("[Veinminer] Created ${this.fileName} default config")
        default
    } else {
        try {
            instance.decodeFromString<T>(readText())
        } catch (e: Exception) {
            println("[Veinminer] Failed to load ${this.fileName} config: Reason: ${e.message}")
            default
        }
    }
}

fun Int.color() = TextColor.color(this)