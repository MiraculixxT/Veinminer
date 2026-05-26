package de.miraculixx.veinminer.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Generic Veinminer custom payload carrying raw JSON-encoded bytes.
 * One [CustomPacketPayload.Type] per channel ID.
 *
 * Wire format is intentionally raw (no length prefix) so the same byte stream is
 * interchangeable with Bukkit plugin messaging on the Paper server.
 */
data class VeinminerPayload(val type: CustomPacketPayload.Type<VeinminerPayload>, val bytes: ByteArray) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<VeinminerPayload> = type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VeinminerPayload) return false
        return type == other.type && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * type.hashCode() + bytes.contentHashCode()
}

/**
 * Reads/writes all readable bytes verbatim, no length prefix. Matches Paper's
 * raw `byte[]` plugin-message wire so a single codec works across loaders.
 */
fun rawBytesCodec(type: CustomPacketPayload.Type<VeinminerPayload>): StreamCodec<RegistryFriendlyByteBuf, VeinminerPayload> =
    object : StreamCodec<RegistryFriendlyByteBuf, VeinminerPayload> {
        override fun decode(buf: RegistryFriendlyByteBuf): VeinminerPayload {
            val bytes = ByteArray(buf.readableBytes())
            buf.readBytes(bytes)
            return VeinminerPayload(type, bytes)
        }

        override fun encode(buf: RegistryFriendlyByteBuf, value: VeinminerPayload) {
            buf.writeBytes(value.bytes)
        }
    }

fun payloadType(channel: String): CustomPacketPayload.Type<VeinminerPayload> =
    CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(NetworkManager.PACKET_IDENTIFIER, channel))
