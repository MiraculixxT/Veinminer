package de.miraculixx.veinminer.config.network

object NetworkManager {
    const val PACKET_IDENTIFIER = "veinminer"

    const val PACKET_JOIN_ID = "join" // c2s
    const val PACKET_MINE_ID = "mine" // c2s
    const val PACKET_KEY_PRESS_ID = "key" // c2s
    const val PACKET_CONFIGURATION_ID = "configuration" // s2c
    const val PACKET_HIGHLIGHT_ID = "highlight" // s2c

}