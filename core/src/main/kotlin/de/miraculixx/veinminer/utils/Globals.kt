package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.ActiveConfig
import de.miraculixx.veinminer.ConfigBridge
import de.miraculixx.veinminer.command.ActiveHost
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.minecraft.resources.Identifier
import de.miraculixx.veinminer.serialization.IdentifierSerializer

const val cRed = 0xff5555
const val cGreen = 0x55ff55
const val cBase = 0xaaaaaa

const val permissionToggle = "veinminer.toggle"
const val permissionBlocks = "veinminer.blocks"
const val permissionSettings = "veinminer.settings"
const val permissionVeinmine = "veinminer.use"
const val permissionGroups = "veinminer.groups"
const val permissionReload = "veinminer.reload"

const val IDENTIFIER = "veinminer"

val debug: Boolean
    get() = ActiveConfig.bridge.settings.debug

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    serializersModule = SerializersModule {
        contextual(Identifier::class, IdentifierSerializer)
    }
}
