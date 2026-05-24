package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.utils.json
import net.minecraft.network.FriendlyByteBuf

object PacketCodecs {
    val JOIN: PacketCodec<JoinInformation> = object : PacketCodec<JoinInformation> {
        override fun read(buf: FriendlyByteBuf) = JoinInformation(buf.readUtf())
        override fun write(buf: FriendlyByteBuf, value: JoinInformation) {
            buf.writeUtf(value.veinminerClientVersion)
        }
    }

    val KEY: PacketCodec<KeyPress> = object : PacketCodec<KeyPress> {
        override fun read(buf: FriendlyByteBuf) = KeyPress(
            pressed = buf.readBoolean(),
            maxDepth = buf.readVarInt(),
            surface = Surface.valueOf(buf.readUtf()),
            patternId = if (buf.readBoolean()) buf.readUtf(MAX_PATTERN_ID_LENGTH) else null,
        )
        override fun write(buf: FriendlyByteBuf, value: KeyPress) {
            buf.writeBoolean(value.pressed)
            buf.writeVarInt(value.maxDepth)
            buf.writeUtf(value.surface.name)
            buf.writeBoolean(value.patternId != null)
            value.patternId?.let { buf.writeUtf(it, MAX_PATTERN_ID_LENGTH) }
        }
    }

    val PATTERNS: PacketCodec<ClientPatternSync> = object : PacketCodec<ClientPatternSync> {
        override fun read(buf: FriendlyByteBuf): ClientPatternSync =
            json.decodeFromString(ClientPatternSync.serializer(), buf.readUtf(MAX_PATTERN_PAYLOAD))
        override fun write(buf: FriendlyByteBuf, value: ClientPatternSync) {
            buf.writeUtf(json.encodeToString(ClientPatternSync.serializer(), value), MAX_PATTERN_PAYLOAD)
        }
    }

    val CONFIGURATION: PacketCodec<ServerConfiguration> = object : PacketCodec<ServerConfiguration> {
        override fun read(buf: FriendlyByteBuf): ServerConfiguration =
            json.decodeFromString(ServerConfiguration.serializer(), buf.readUtf(MAX_CONFIG_PAYLOAD))
        override fun write(buf: FriendlyByteBuf, value: ServerConfiguration) {
            buf.writeUtf(json.encodeToString(ServerConfiguration.serializer(), value), MAX_CONFIG_PAYLOAD)
        }
    }

    private const val MAX_CONFIG_PAYLOAD = 1 shl 20 // 1 MiB headroom for large group sets
    private const val MAX_PATTERN_PAYLOAD = 64 * 1024
    private const val MAX_PATTERN_ID_LENGTH = 64
}
