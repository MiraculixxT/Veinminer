package de.miraculixx.veinminer.config

data class ParsedData<T>(
    val parsed: Set<T>,
    val invalid: Set<String>
)

enum class MaterialType {
    ITEM,
    BLOCK
}
