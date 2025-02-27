package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.config.data.FixedBlockGroup
import de.miraculixx.veinminer.config.extensions.toVeinminer
import de.miraculixx.veinminer.config.network.JoinInformation
import de.miraculixx.veinminer.config.network.RequestBlockVein
import de.miraculixx.veinminer.config.pattern.Pattern
import de.miraculixx.veinminerClient.constants.PACKET_CONFIGURATION
import de.miraculixx.veinminerClient.constants.PACKET_HIGHLIGHT
import de.miraculixx.veinminerClient.constants.PACKET_JOIN
import de.miraculixx.veinminerClient.constants.PACKET_MINE
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component

object NetworkManager {
    // Client info
    var isVeinminerActive = false
        private set
    var selectedPattern = Pattern.DEFAULT

    // Server info
    var mustSneak = false
        private set
    var cooldown = 0
        private set
    var globalBlocks = setOf<String>()
        private set
    var blockGroups = setOf<FixedBlockGroup>()
        private set

    private val onConfiguration = PACKET_CONFIGURATION.receiveOnClient { packet, context ->
        if (packet.outdated) {
            context.client.toastManager.addToast(
                SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("Veinminer Outdated"), Component.literal("Please update Veinminer"))
            )
        }

        isVeinminerActive = true
        mustSneak = packet.mustSneak
        cooldown = packet.cooldown
        globalBlocks = packet.globalBlockList
        blockGroups = packet.blockGroups
    }

    private val onHighlight = PACKET_HIGHLIGHT.receiveOnClient { packet, context ->
        if (!packet.allowed) {
            // TODO
            return@receiveOnClient
        }

        BlockHighlightingRenderer.highlightedBlocks.clear()
        BlockHighlightingRenderer.highlightedBlocks.addAll(packet.blocks)
    }

    fun requestBlockInfo(position: BlockPos, direction: Direction) {
        PACKET_MINE.send(RequestBlockVein(position.toVeinminer(), direction.toVeinminer(), selectedPattern, false))
    }

    fun requestBlockMine(position: BlockPos, direction: Direction) {
        PACKET_MINE.send(RequestBlockVein(position.toVeinminer(), direction.toVeinminer(), selectedPattern, true))
    }

    fun sendJoin(version: String) {
        PACKET_JOIN.send(JoinInformation(version))
    }
}