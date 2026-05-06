package de.miraculixx.veinminer.config

/**
 * Loader-specific bridge between raw config strings and native registry types.
 * Implementations resolve a single id ("minecraft:stone") or a tag ("#minecraft:logs")
 * into the platform's identifier type.
 */
interface ConfigSerializer<T> {
    fun parseList(rawList: Set<String>, type: MaterialType): ParsedData<T>
}
