package de.miraculixx.veinminer.network

import java.util.UUID

/**
 * Loader-specific actions that touch platform world/block types.
 * The common [NetworkRouter] decodes the payload and invokes these.
 */
interface ServerCallbacks {
    fun onJoinAccepted(playerId: UUID, packet: JoinInformation)
    fun onMineRequest(playerId: UUID, packet: RequestBlockVein)
    fun onKeyPress(playerId: UUID, packet: KeyPress)
}
