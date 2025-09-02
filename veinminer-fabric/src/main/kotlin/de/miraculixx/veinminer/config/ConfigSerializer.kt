package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.VeinMinerEvent.key
import de.miraculixx.veinminer.Veinminer
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.silkmc.silk.core.Silk.server

object ConfigSerializer {

    /**
     * Parses the raw config input out of strings into a complete list of namespaced keys,
     * respecting single materials and tags.
     * Invalid entries are ignored.
     * @param type Used to specify if it's a block or item tag
     */
    fun parseList(rawList: Set<String>, type: MaterialType): ParsedData {
        val parsed = mutableSetOf<ResourceLocation>()
        val invalid = mutableSetOf<String>()

        rawList.forEach { raw ->
            val entries = parseEntry(raw, type)
            if (entries == null) {
                Veinminer.LOGGER.warn("Failed to access server registry! Cannot parse config, Veinminer will be inactive!")

            } else if (entries.isEmpty()) { // Remove invalid entries
                invalid.add(raw)
                Veinminer.LOGGER.warn("Invalid ${type.name.lowercase()} entry in config: $raw")

            } else parsed.addAll(entries)
        }
        return ParsedData(parsed, invalid)
    }

    /**
     * Turns any raw input either into a single material, all materials in a tag or empty list if invalid
     * @param type Used to specify if it's a block or item tag
     */
    private fun parseEntry(raw: String, type: MaterialType): Set<ResourceLocation>? {
        val isTag = raw.startsWith("#")
        val name = if (isTag) raw.substring(1) else raw // remove trailing #

        // Parse raw into a NamespacedKey
        val key = ResourceLocation.tryParse(name) ?: return emptySet()

        return if (isTag) { // '#minecraft:logs' tag parsed into all materials in that tag
            when (type) {
                MaterialType.ITEM -> {
                    val tag = TagKey.create(Registries.ITEM, key)
                    val reg = server?.registryAccess()?.lookupOrThrow(Registries.ITEM) ?: return null
                    reg.getTagOrEmpty(tag).map { it.value().defaultInstance.key() }.toSet()
                }

                MaterialType.BLOCK -> {
                    val tag = TagKey.create(Registries.BLOCK, key)
                    val reg = server?.registryAccess()?.lookupOrThrow(Registries.BLOCK) ?: return null
                    reg.getTagOrEmpty(tag).map { it.value().key() }.toSet()
                }
            }

        } else { // 'minecraft:stone' single material check if existing and return
            val access = server?.registryAccess() ?: return null
            val reg = if (type == MaterialType.BLOCK) access.lookupOrThrow(Registries.BLOCK) else access.lookupOrThrow(Registries.ITEM)
            if (reg.containsKey(key)) setOf(key) else emptySet()
        }
    }

    enum class MaterialType {
        ITEM,
        BLOCK
    }

    data class ParsedData(
        val parsed: Set<ResourceLocation>,
        val invalid: Set<String>
    )
}