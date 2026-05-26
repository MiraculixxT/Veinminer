package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.utils.mcServer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

/**
 * Fabric & NeoForge exclusive - Paper carries its own impl
 */
object IdentifierConfigSerializer : ConfigSerializer<ResourceLocation> {

    override fun parseList(rawList: Set<String>, type: MaterialType): ParsedData<ResourceLocation> {
        val parsed = mutableSetOf<ResourceLocation>()
        val invalid = mutableSetOf<String>()
        val logger = ActiveHost.host.logger

        rawList.forEach { raw ->
            val entries = parseEntry(raw, type)
            if (entries == null) {
                logger.warn("Failed to access server registry! Cannot parse config, Veinminer will be inactive!")

            } else if (entries.isEmpty()) {
                invalid.add(raw)
                logger.warn("Invalid ${type.name.lowercase()} entry in config: $raw")

            } else parsed.addAll(entries)
        }
        return ParsedData(parsed, invalid)
    }

    private fun parseEntry(raw: String, type: MaterialType): Set<ResourceLocation>? {
        val isTag = raw.startsWith("#")
        val name = if (isTag) raw.substring(1) else raw

        val key = ResourceLocation.tryParse(name) ?: return emptySet()

        return if (isTag) {
            when (type) {
                MaterialType.ITEM -> {
                    val tag = TagKey.create(Registries.ITEM, key)
                    val reg = mcServer?.registryAccess()?.lookupOrThrow(Registries.ITEM) ?: return null
                    reg.get(tag).orElse(null)?.map { BuiltInRegistries.ITEM.getKey(it.value()) }?.toSet() ?: emptySet()
                }

                MaterialType.BLOCK -> {
                    val tag = TagKey.create(Registries.BLOCK, key)
                    val reg = mcServer?.registryAccess()?.lookupOrThrow(Registries.BLOCK) ?: return null
                    reg.get(tag).orElse(null)?.map { BuiltInRegistries.BLOCK.getKey(it.value()) }?.toSet() ?: emptySet()
                }
            }

        } else {
            val access = mcServer?.registryAccess() ?: return null
            val exists = if (type == MaterialType.BLOCK) {
                access.lookupOrThrow(Registries.BLOCK).get(ResourceKey.create(Registries.BLOCK, key)).isPresent
            } else {
                access.lookupOrThrow(Registries.ITEM).get(ResourceKey.create(Registries.ITEM, key)).isPresent
            }
            if (exists) setOf(key) else emptySet()
        }
    }
}
