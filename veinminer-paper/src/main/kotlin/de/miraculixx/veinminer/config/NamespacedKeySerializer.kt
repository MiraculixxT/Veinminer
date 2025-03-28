package de.miraculixx.veinminer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey

object NamespacedKeySerializer : KSerializer<NamespacedKey> {
    override val descriptor = PrimitiveSerialDescriptor("NamespacedKey", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NamespacedKey {
        val decode = decoder.decodeString()
        val split = decode.split(":")
        return if (split.size != 2) NamespacedKey("minecraft", decode.lowercase())
        else NamespacedKey(split[0], split[1])
    }

    override fun serialize(encoder: Encoder, value: NamespacedKey) {
        encoder.encodeString(value.toString())
    }
}