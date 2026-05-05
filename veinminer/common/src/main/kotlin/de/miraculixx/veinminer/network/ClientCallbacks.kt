package de.miraculixx.veinminer.network

/**
 * Client-side handlers for decoded S2C packets
 */
interface ClientCallbacks {
    fun onConfiguration(packet: ServerConfiguration)
    fun onHighlight(packet: BlockHighlighting)
}
