package de.miraculixx.veinminer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.ResourceLocation

object ResourceLocationSerializer : KSerializer<ResourceLocation> {
    override val descriptor = PrimitiveSerialDescriptor("ResourceLocation", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ResourceLocation {
        var decode = decoder.decodeString()
        return if (!decode.contains(":")) {
            decode = decode.removePrefix("block.").removePrefix("item.").replace('.', ':')
            ResourceLocation.parse(decode)
        } else ResourceLocation.parse(decode)
    }

    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeString(value.toString())
    }
}