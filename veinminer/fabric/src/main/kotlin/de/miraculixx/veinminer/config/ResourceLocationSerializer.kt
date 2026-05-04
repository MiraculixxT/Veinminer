package de.miraculixx.veinminer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.Identifier

object ResourceLocationSerializer : KSerializer<Identifier> {
    override val descriptor = PrimitiveSerialDescriptor("ResourceLocation", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Identifier {
        var decode = decoder.decodeString()
        return if (!decode.contains(":")) {
            decode = decode.removePrefix("block.").removePrefix("item.").replace('.', ':')
            Identifier.parse(decode)
        } else Identifier.parse(decode)
    }

    override fun serialize(encoder: Encoder, value: Identifier) {
        encoder.encodeString(value.toString())
    }
}