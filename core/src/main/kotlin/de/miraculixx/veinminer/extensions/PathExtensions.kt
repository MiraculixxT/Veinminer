package de.miraculixx.veinminer.extensions

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.utils.json
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.format.TextColor
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

inline fun <reified T> Path.load(default: T, instance: Json = json): T {
    return if (!exists()) {
        createParentDirectories()
        val string = instance.encodeToString(default)
        writeText(string)
        ActiveHost.host.logger.info("Created ${this.fileName} default config")
        default
    } else {
        try {
            instance.decodeFromString<T>(readText())
        } catch (e: Exception) {
            ActiveHost.host.logger.warn("Failed to load ${this.fileName} config: Reason: ${e.message}")
            default
        }
    }
}

fun Int.color() = TextColor.color(this)