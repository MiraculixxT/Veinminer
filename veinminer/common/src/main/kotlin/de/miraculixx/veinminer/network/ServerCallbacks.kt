package de.miraculixx.veinminer.network

import java.util.UUID

/**
 * Server-side handlers for decoded C2S packets
 */
interface ServerCallbacks {
    fun onJoinAccepted(playerId: UUID, packet: JoinInformation)
    fun onMineRequest(playerId: UUID, packet: RequestBlockVein)
    fun onKeyPress(playerId: UUID, packet: KeyPress)
}
