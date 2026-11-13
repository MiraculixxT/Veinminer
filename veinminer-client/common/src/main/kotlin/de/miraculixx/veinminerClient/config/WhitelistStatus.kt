package de.miraculixx.veinminerClient.config

import de.miraculixx.veinminer.pattern.BlockFilter
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.cYellow
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Client side evaluation of a whitelist entry against the server config
 */
data class WhitelistStatus(val state: State, val blocked: Int = 0, val total: Int = 0) {
    enum class State(val severity: Int) { ALLOWED(0), PARTIAL(1), BLOCKED(2), NO_SERVER(0), INVALID(2) }

    fun message(entry: String): MutableComponent? = when (state) {
        State.ALLOWED, State.NO_SERVER -> null
        State.PARTIAL -> Component.literal("⚠ $blocked of $total blocks in $entry are not allowed by the server").withColor(cYellow)
        State.BLOCKED -> Component.literal("⚠ $entry is not allowed by the server and will be ignored").withColor(cRed)
        State.INVALID -> Component.literal("Unknown block or tag: $entry").withColor(cRed)
    }

    companion object {
        private val cache = HashMap<String, WhitelistStatus>()

        fun cached(entry: String): WhitelistStatus = cache.getOrPut(entry) { of(entry) }

        /** Call when server config or world changes */
        fun clearCache() = cache.clear()

        /** Most severe state of [entries] that needs attention, null if everything is fine */
        fun worst(entries: Collection<String>): State? =
            entries.map { cached(it).state }.filter { it.severity > 0 }.maxByOrNull { it.severity }

        fun of(entry: String): WhitelistStatus {
            val id = BlockFilter.parse(entry) ?: return WhitelistStatus(State.INVALID)
            val inWorld = Minecraft.getInstance().level != null
            val resolved = if (BlockFilter.isTag(entry)) {
                // Tags are only bound while connected, outside a world they can not be validated
                if (!inWorld) return WhitelistStatus(State.NO_SERVER)
                if (BuiltInRegistries.BLOCK.getTag(BlockFilter.tagKey(id)).isEmpty) return WhitelistStatus(State.INVALID)
                BlockFilter.resolveEntry(entry)
            } else {
                if (!BuiltInRegistries.BLOCK.containsKey(id)) return WhitelistStatus(State.INVALID)
                setOf(id)
            }
            if (!inWorld || !NetworkManager.isVeinminerActive) return WhitelistStatus(State.NO_SERVER)
            if (NetworkManager.settings.client.allBlocks) return WhitelistStatus(State.ALLOWED, 0, resolved.size)
            val blocked = resolved.count { it !in NetworkManager.allowedBlocks }
            val state = when {
                blocked == 0 -> State.ALLOWED
                blocked < resolved.size -> State.PARTIAL
                else -> State.BLOCKED
            }
            return WhitelistStatus(state, blocked, resolved.size)
        }
    }
}
