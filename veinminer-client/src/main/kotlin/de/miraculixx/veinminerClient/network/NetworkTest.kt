package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.config.network.NetworkManager
import de.miraculixx.veinminer.config.utils.IDENTIFIER
import de.miraculixx.veinminerClient.VeinminerClient
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

object NetworkTest: CustomPacketPayload {
    val ID: CustomPacketPayload.Type<NetworkTest> = CustomPacketPayload.Type(ResourceLocation.parse("$IDENTIFIER:${NetworkManager.PACKET_CONFIGURATION_ID}"))

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?>? {
        return ID
    }

    init {
        ClientPlayNetworking.registerGlobalReceiver(ID) { payload, context ->
            VeinminerClient.LOGGER.info("NetworkTest: $payload")
        }
    }
}