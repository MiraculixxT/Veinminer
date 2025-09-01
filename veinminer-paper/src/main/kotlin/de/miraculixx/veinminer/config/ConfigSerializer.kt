package de.miraculixx.veinminer.config

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey

object ConfigSerializer {

    /**
     * Parses the raw config input out of strings into a complete list of namespaced keys,
     * respecting single materials and tags.
     * Invalid entries are ignored.
     * @param type Used to specify if it's a block or item tag
     */
    fun parseList(rawList: Set<String>, type: MaterialType): Set<NamespacedKey> {
        return buildSet {
            rawList.forEach { raw ->
                addAll(parseEntry(raw, type))
            }
        }
    }

    /**
     * Turns any raw input either into a single material, all materials in a tag or empty list if invalid
     * @param type Used to specify if it's a block or item tag
     */
    private fun parseEntry(raw: String, type: MaterialType): Set<NamespacedKey> {
        val isTag = raw.startsWith("#")
        val name = if (isTag) raw.substring(1) else raw // remove trailing #

        // Parse raw into a NamespacedKey
        val key = NamespacedKey.fromString(name) ?: return emptySet()

        return if (isTag) { // '#minecraft:logs' tag parsed into all materials in that tag
            val tag = Bukkit.getTag(type.regis, key, Material::class.java) ?: return emptySet()
            tag.values.map { it.key }.toSet()

        } else { // 'minecraft:stone' single material just returned
            setOf(key)
        }
    }

    enum class MaterialType(val regis: String) {
        ITEM("items"),
        BLOCK("blocks")
    }
}