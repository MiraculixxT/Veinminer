package de.miraculixx.veinminer.config

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersion(
    val version_number: String,
    val files: List<ModrinthFile>
)

@Serializable
data class ModrinthFile(
    val url: String,
    val filename: String,
    val size: Long
)
