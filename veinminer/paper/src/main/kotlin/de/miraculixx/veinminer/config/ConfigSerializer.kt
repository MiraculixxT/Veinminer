package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.Veinminer
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey

object PaperConfigSerializer : ConfigSerializer<NamespacedKey> {

    override fun parseList(rawList: Set<String>, type: MaterialType): ParsedData<NamespacedKey> {
        val parsed = mutableSetOf<NamespacedKey>()
        val invalid = mutableSetOf<String>()

        rawList.forEach { raw ->
            val entries = parseEntry(raw, type)
            if (entries.isEmpty()) {
                invalid.add(raw)
                Veinminer.LOGGER.warn("Invalid ${type.name.lowercase()} entry in config: $raw")

            } else parsed.addAll(entries)
        }
        return ParsedData(parsed, invalid)
    }

    private fun parseEntry(raw: String, type: MaterialType): Set<NamespacedKey> {
        val isTag = raw.startsWith("#")
        val name = if (isTag) raw.substring(1) else raw

        val key = NamespacedKey.fromString(name) ?: return emptySet()

        return if (isTag) {
            val tag = Bukkit.getTag(type.registry, key, Material::class.java) ?: return emptySet()
            tag.values.map { it.key }.toSet()

        } else {
            val regAccess = RegistryAccess.registryAccess()
            val reg = if (type == MaterialType.BLOCK) regAccess.getRegistry(RegistryKey.BLOCK) else regAccess.getRegistry(RegistryKey.ITEM)
            if (reg.get(key) != null) setOf(key) else emptySet()
        }
    }

    private val MaterialType.registry: String
        get() = if (this == MaterialType.ITEM) "items" else "blocks"
}
