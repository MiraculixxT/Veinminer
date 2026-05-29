package de.miraculixx.veinminer

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.data.ModrinthFile
import de.miraculixx.veinminer.data.ModrinthVersion
import de.miraculixx.veinminer.utils.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object UpdateManager {
    private const val DEBUG = true
    private val logger = ActiveHost.host.logger

    fun checkForUpdates(module: Module, platform: String, serverVersion: String, modVersion: String?): VersionInfo {
        val target = URI("https://api.modrinth.com/v2/project/${module.id}/version?loaders=%5B%22${platform}%22%5D&game_versions=%5B%22$serverVersion%22%5D").toURL()
        val con = target.openConnection() as HttpURLConnection
        val content = con.inputStream.readAllBytes().decodeToString()
        val latest = json.decodeFromString<List<ModrinthVersion>>(content).firstOrNull()
        if (latest == null) {
            logger.warn("No version found for ${module.modID}${if (DEBUG) " (${target.path})" else ""}")
            return VersionInfo(false, modVersion ?: "unknown", "unknown", module)
        }

        val outdated = if (latest.version_number == modVersion) {
            logger.info("${module.modID} is up to date")
            false
        } else if (modVersion != null) {
            logger.warn("${module.modID} is outdated ($serverVersion). Installed: $modVersion -> Latest: ${latest.version_number}")
            true
        } else false

        if (DEBUG) update(module, latest.files.first(), platform)

        return VersionInfo(outdated, modVersion ?: "unknown", latest.version_number, module)
    }

    private fun update(module: Module, file: ModrinthFile, platform: String) {
        val target = URI(file.url).toURL()
        val con = target.openConnection() as HttpURLConnection
        val content = con.inputStream.readAllBytes()
        val configFolder = File(if (platform != "paper") "config/Veinminer" else "plugins/Veinminer")
        val targetFolder = File(configFolder, "update").apply { mkdirs() }
        val targetFile = File(targetFolder, file.filename)
        targetFile.writeBytes(content)
    }

    /**
     * Launches a coroutine that checks each module against Modrinth and invokes
     * [onOutdated] for every module that is behind the latest published version.
     * [moduleVersionLookup] returns the currently installed version of a module,
     * or null if the module isn't installed.
     */
    fun startUpdateChecker(
        modules: List<Module>,
        platform: String,
        serverVersion: String,
        moduleVersionLookup: (Module) -> String?,
        onOutdated: (VersionInfo) -> Unit,
    ): Job = CoroutineScope(Dispatchers.Default).launch {
        modules.forEach { module ->
            try {
                val info = checkForUpdates(module, platform, serverVersion, moduleVersionLookup(module))
                if (info.outdated) onOutdated(info)
            } catch (e: Exception) {
                logger.warn("Error while checking for updates: ${e.message}")
            }
        }
    }

    enum class Module(val id: String, val modID: String, val cfID: String) {
        VEINMINER("OhduvhIc", "veinminer", "veinminer-mod"),
        VEINMINER_ENCHANTMENT("4sP0LXxp", "veinminer-enchantment", "veinminer-enchant"),
        VEINMINER_CLIENT("dxa0Bm8m", "veinminer-client", "veinminer-hotkey"),
    }

    data class VersionInfo(
        val outdated: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val module: Module
    )
}