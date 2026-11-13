package de.miraculixx.veinminer.pattern

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block

/**
 * Client defined block whitelist per pattern. Entries are block ids or block tags prefixed with `#`.
 * It can only reduce the server target set, never extend it.
 */
object BlockFilter {
    /** Resolved client whitelist. [combine] treats it as own block group */
    data class ClientFilter(val blocks: Set<Identifier>, val combine: Boolean)

    const val MAX_ENTRIES = 64
    const val MAX_ENTRY_LENGTH = 128

    fun isTag(raw: String): Boolean = raw.startsWith("#")

    fun parse(raw: String): Identifier? = Identifier.tryParse(raw.trim().removePrefix("#"))

    fun isValidEntry(raw: String): Boolean = raw.isNotBlank() && raw.length <= MAX_ENTRY_LENGTH && parse(raw) != null

    /** Normalizes user input (e.g. `stone` -> `minecraft:stone`, `#logs` -> `#minecraft:logs`) */
    fun normalize(raw: String): String? {
        val id = parse(raw) ?: return null
        return if (isTag(raw.trim())) "#$id" else id.toString()
    }

    fun tagKey(id: Identifier): TagKey<Block> = TagKey.create(Registries.BLOCK, id)

    fun resolveEntry(raw: String): Set<Identifier> {
        val id = parse(raw) ?: return emptySet()
        return if (isTag(raw.trim())) {
            BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey(id)).mapTo(mutableSetOf()) { BuiltInRegistries.BLOCK.getKey(it.value()) }
        } else if (BuiltInRegistries.BLOCK.containsKey(id)) setOf(id) else emptySet()
    }

    fun resolve(entries: Collection<String>): Set<Identifier> = entries.flatMapTo(mutableSetOf(), ::resolveEntry)

    /**
     * Restricts [targets] to the [filter]. Null filter = no restriction.
     * Combining uses the filter as target group, but only keeps blocks passing [canCombine] (server whitelist, tools).
     * @return null if the origin block is not part of the filter
     */
    fun apply(
        filter: ClientFilter?,
        origin: Identifier,
        targets: Set<Identifier>,
        allowCombine: Boolean,
        canCombine: (Identifier) -> Boolean,
    ): Set<Identifier>? {
        if (filter == null) return targets
        if (origin !in filter.blocks) return null
        if (filter.combine && allowCombine) return filter.blocks.filterTo(mutableSetOf()) { it == origin || canCombine(it) }
        return targets.filterTo(mutableSetOf()) { it in filter.blocks }
    }
}
