package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.data.ModrinthFile
import de.miraculixx.veinminer.config.data.ModrinthVersion
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object UpdateManager {
    private const val DEBUG = true

    fun checkForUpdates(module: Module, platform: String, serverVersion: String, modVersion: String?) {
        val target = URI("https://api.modrinth.com/v2/project/${module.id}/version?loaders=%5B%22${platform}%22%5D&game_versions=%5B%22$serverVersion%22%5D").toURL()
        val con = target.openConnection() as HttpURLConnection
        val content = con.inputStream.readAllBytes().decodeToString()
        val latest = json.decodeFromString<List<ModrinthVersion>>(content).firstOrNull()
            ?: return println("[VeinminerUpdater] No version found for ${module.modID}${if (DEBUG) " (${target.path})" else ""}")

        if (latest.version_number == modVersion) println("[VeinminerUpdater] ${module.modID} is up to date")
        else if (modVersion != null) println("[VeinminerUpdater] ${module.modID} is outdated. Installed: $modVersion -> Latest: ${latest.version_number}")

        if (DEBUG) update(module, latest.files.first(), platform)
    }

    private fun update(module: Module, file: ModrinthFile, platform: String) {
        val target = URI(file.url).toURL()
        val con = target.openConnection() as HttpURLConnection
        val content = con.inputStream.readAllBytes()
        val configFolder = File(if (platform == "fabric") "config/veinminer" else "plugins/Veinminer")
        val targetFolder = File(configFolder, "update").apply { mkdirs() }
        val targetFile = File(targetFolder, file.filename)
        targetFile.writeBytes(content)
    }

    enum class Module(val id: String, val modID: String) {
        VEINMINER("OhduvhIc", "veinminer"),
        VEINMINER_ENCHANTMENT("4sP0LXxp", "veinminer-enchantment"),
        VEINMINER_CLIENT("4sP0LXxp", "veinminer-client")
    }
}