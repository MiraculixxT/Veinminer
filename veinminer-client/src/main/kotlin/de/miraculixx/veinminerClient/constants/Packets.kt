package de.miraculixx.veinminerClient.constants

import de.miraculixx.veinminer.config.network.BlockHighlighting
import de.miraculixx.veinminer.config.network.JoinInformation
import de.miraculixx.veinminer.config.network.NetworkManager
import de.miraculixx.veinminer.config.network.RequestBlockVein
import de.miraculixx.veinminer.config.network.ServerConfiguration
import net.minecraft.resources.ResourceLocation
import net.silkmc.silk.network.packet.c2sPacket
import net.silkmc.silk.network.packet.s2cPacket

val PACKET_JOIN = c2sPacket<JoinInformation>(ResourceLocation.fromNamespaceAndPath(NetworkManager.PACKET_IDENTIFIER, NetworkManager.PACKET_JOIN_ID))
val PACKET_MINE = c2sPacket<RequestBlockVein>(ResourceLocation.fromNamespaceAndPath(NetworkManager.PACKET_IDENTIFIER, NetworkManager.PACKET_MINE_ID))
val PACKET_CONFIGURATION = s2cPacket<ServerConfiguration>(ResourceLocation.fromNamespaceAndPath(NetworkManager.PACKET_IDENTIFIER, NetworkManager.PACKET_CONFIGURATION_ID))
val PACKET_HIGHLIGHT = s2cPacket<BlockHighlighting>(ResourceLocation.fromNamespaceAndPath(NetworkManager.PACKET_IDENTIFIER, NetworkManager.PACKET_HIGHLIGHT_ID))
