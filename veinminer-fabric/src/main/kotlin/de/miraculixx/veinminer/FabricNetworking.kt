package de.miraculixx.veinminer

import de.miraculixx.veinminer.config.IDENTIFIER
import de.miraculixx.veinminer.config.NetworkManager
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

//object FabricNetworking: CustomPacketPayload {
//    val PACKET_IDENTIFIER = ResourceLocation.fromNamespaceAndPath(IDENTIFIER, NetworkManager.PACKET_JOIN)
//    val PACKET_ID = CustomPacketPayload.Type<FabricNetworking>(PACKET_IDENTIFIER)
//    val PACKET_CODEC = StreamCodec.composite<RegistryFriendlyByteBuf,  FabricNetworking>()
//
//    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?>? {
//        return PACKET_ID
//    }
//}