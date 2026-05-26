package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.Veinminer
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
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
            val tag = Bukkit.getTag(type.registry, key, Material::class.java)
            tag?.values?.map { it.key }?.toSet() ?: parseBundledTag(key, type, mutableSetOf())

        } else {
            val regAccess = RegistryAccess.registryAccess()
            val reg = if (type == MaterialType.BLOCK) regAccess.getRegistry(RegistryKey.BLOCK) else regAccess.getRegistry(RegistryKey.ITEM)
            if (reg.get(key) != null) setOf(key) else emptySet()
        }
    }

    private val MaterialType.registry: String
        get() = if (this == MaterialType.ITEM) "items" else "blocks"

    private fun parseBundledTag(key: NamespacedKey, type: MaterialType, seen: MutableSet<NamespacedKey>): Set<NamespacedKey> {
        if (!seen.add(key)) return emptySet()

        val path = "veinminer_dp/data/${key.namespace}/tags/${type.datapackRegistry}/${key.key}.json"
        val stream = this::class.java.classLoader.getResourceAsStream(path) ?: return emptySet()
        val root = stream.use { Json.parseToJsonElement(it.readAllBytes().decodeToString()).jsonObject }
        val values = root["values"] as? JsonArray ?: return emptySet()

        return values.flatMapTo(mutableSetOf()) { value ->
            val raw = value.tagValue() ?: return@flatMapTo emptySet()
            if (raw.startsWith("#")) {
                val nested = NamespacedKey.fromString(raw.substring(1)) ?: return@flatMapTo emptySet()
                parseBundledTag(nested, type, seen)
            } else {
                val entry = NamespacedKey.fromString(raw) ?: return@flatMapTo emptySet()
                if (exists(entry, type)) setOf(entry) else emptySet()
            }
        }
    }

    private fun exists(key: NamespacedKey, type: MaterialType): Boolean {
        val regAccess = RegistryAccess.registryAccess()
        val reg = if (type == MaterialType.BLOCK) regAccess.getRegistry(RegistryKey.BLOCK) else regAccess.getRegistry(RegistryKey.ITEM)
        return reg.get(key) != null
    }

    private fun JsonElement.tagValue(): String? = when (this) {
        is JsonPrimitive -> content
        is JsonObject -> (this["id"] as? JsonPrimitive)?.content
        else -> null
    }

    private val MaterialType.datapackRegistry: String
        get() = if (this == MaterialType.ITEM) "item" else "block"
}
