package de.miraculixx.veinminer.network

object NetworkManager {
    const val PACKET_IDENTIFIER = "veinminer"

    /*
     * Protocol channels:
     * - join: client -> server, sent once after joining to announce the client addon version.
     * - key: client -> server, sent when the hotkey state, shape, depth, or target surface changes.
     * - configuration: server -> client, sent after join and after server config reloads.
     */
    const val PACKET_JOIN_ID = "join" // c2s
    const val PACKET_KEY_PRESS_ID = "key" // c2s
    const val PACKET_CONFIGURATION_ID = "configuration" // s2c
}
