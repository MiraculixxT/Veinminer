package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
import java.util.Collections

object HighlightCache {
    private const val TTL_MS = 3000L
    private const val MAX_ENTRIES = 256

    data class Key(
        val world: Any,
        val pos: BlockPosition,
        val blockKey: String,
        val shape: Shape,
        val surface: Surface,
    )

    private data class Entry(val blocks: List<BlockPosition>, val toolIcon: String, val insertedMs: Long)

    private val store: MutableMap<Key, Entry> = Collections.synchronizedMap(
        object : LinkedHashMap<Key, Entry>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Entry>): Boolean {
                return size > MAX_ENTRIES
            }
        }
    )

    fun get(key: Key): Pair<List<BlockPosition>, String>? {
        val entry = synchronized(store) { store[key] } ?: return null
        if (System.currentTimeMillis() - entry.insertedMs > TTL_MS) {
            synchronized(store) { store.remove(key) }
            return null
        }
        return entry.blocks to entry.toolIcon
    }

    fun put(key: Key, blocks: List<BlockPosition>, toolIcon: String) {
        synchronized(store) { store[key] = Entry(blocks, toolIcon, System.currentTimeMillis()) }
    }

    fun invalidate(world: Any, pos: BlockPosition) {
        synchronized(store) {
            val it = store.keys.iterator()
            while (it.hasNext()) {
                val k = it.next()
                if (k.world === world && k.pos == pos) it.remove()
            }
        }
    }

    fun clear() {
        synchronized(store) { store.clear() }
    }
}
