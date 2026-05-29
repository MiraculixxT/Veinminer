package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.ActiveConfig
import kotlinx.serialization.json.Json

const val cRed = 0xff5555
const val cGreen = 0x55ff55
const val cBase = 0xaaaaaa
const val cHighlight = 0x5CA0D4
const val cWhite = 0xffffff

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
}
