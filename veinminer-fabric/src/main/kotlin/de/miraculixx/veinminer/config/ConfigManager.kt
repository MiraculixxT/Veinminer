package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.config.data.BlockGroup
import de.miraculixx.veinminer.config.data.VeinminerSettings
import de.miraculixx.veinminer.config.extensions.load
import de.miraculixx.veinminer.networking.FabricNetworking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.resources.ResourceLocation
import net.silkmc.silk.core.Silk
import kotlin.io.path.Path
import kotlin.io.path.writeText

object ConfigManager {
    private val blocksFile = Path("config/Veinminer/blocks.json")
    private val settingsFile = Path("config/Veinminer/settings.json")
    private val groupsFile = Path("config/Veinminer/groups.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(ResourceLocation::class, ResourceLocationSerializer)
        }
    }

    var veinBlocks: MutableSet<ResourceLocation> = loadBlocks()
        private set

    var settings: VeinminerSettings = loadSettings()
        private set

    var groups: MutableSet<BlockGroup<ResourceLocation>> = loadGroups()
        private set


    fun reload() {
        settings = loadSettings()
        veinBlocks = loadBlocks()
        groups = loadGroups()
    }

    fun save() {
        settingsFile.writeText(json.encodeToString(settings))
        blocksFile.writeText(json.encodeToString(veinBlocks))
        groupsFile.writeText(json.encodeToString(groups))

        Silk.players.forEach { player -> FabricNetworking.sendConfiguration(player, settings) }
    }

    private fun loadSettings() = settingsFile.load<VeinminerSettings>(VeinminerSettings())
    private fun loadGroups(): MutableSet<BlockGroup<ResourceLocation>> {
        val stream = this::class.java.classLoader.getResourceAsStream("default_groups.json") ?: throw IllegalStateException("[Veinminer] Could not find and load default_groups.json")
        val defaultSource = stream.readAllBytes().decodeToString()
        stream.close()
        val defaultGroups = json.decodeFromString<MutableSet<BlockGroup<ResourceLocation>>>(defaultSource)
        return groupsFile.load<MutableSet<BlockGroup<ResourceLocation>>>(defaultGroups, json)
    }

    private fun loadBlocks() = blocksFile.load<MutableSet<ResourceLocation>>(mutableSetOf(), json)

}