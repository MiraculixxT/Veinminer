package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.pattern.Shape
import de.miraculixx.veinminer.pattern.Surface
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
            shape = Shape.valueOf(buf.readUtf())
        )
        override fun write(buf: FriendlyByteBuf, value: KeyPress) {
            buf.writeBoolean(value.pressed)
            buf.writeUtf(value.shape.name)
        }
    }

    val MINE: PacketCodec<RequestBlockVein> = object : PacketCodec<RequestBlockVein> {
        override fun read(buf: FriendlyByteBuf) = RequestBlockVein(
            blockPosition = readBlockPosition(buf),
            surface = Surface.valueOf(buf.readUtf())
        )
        override fun write(buf: FriendlyByteBuf, value: RequestBlockVein) {
            writeBlockPosition(buf, value.blockPosition)
            buf.writeUtf(value.surface.name)
        }
    }

    val CONFIGURATION: PacketCodec<ServerConfiguration> = object : PacketCodec<ServerConfiguration> {
        override fun read(buf: FriendlyByteBuf) = ServerConfiguration(
            cooldown = buf.readVarInt(),
            mustSneak = buf.readBoolean(),
            outdated = buf.readBoolean(),
            translucentBlockHighlight = buf.readBoolean()
        )
        override fun write(buf: FriendlyByteBuf, value: ServerConfiguration) {
            buf.writeVarInt(value.cooldown)
            buf.writeBoolean(value.mustSneak)
            buf.writeBoolean(value.outdated)
            buf.writeBoolean(value.translucentBlockHighlight)
        }
    }

    val HIGHLIGHT: PacketCodec<BlockHighlighting> = object : PacketCodec<BlockHighlighting> {
        override fun read(buf: FriendlyByteBuf) = BlockHighlighting(
            allowed = buf.readBoolean(),
            icon = buf.readUtf(),
            blocks = buf.readList { readBlockPosition(it) }
        )
        override fun write(buf: FriendlyByteBuf, value: BlockHighlighting) {
            buf.writeBoolean(value.allowed)
            buf.writeUtf(value.icon)
            buf.writeCollection(value.blocks) { b, pos -> writeBlockPosition(b, pos) }
        }
    }

    private fun readBlockPosition(buf: FriendlyByteBuf): BlockPosition =
        BlockPosition(buf.readVarInt(), buf.readVarInt(), buf.readVarInt())

    private fun writeBlockPosition(buf: FriendlyByteBuf, pos: BlockPosition) {
        buf.writeVarInt(pos.x)
        buf.writeVarInt(pos.y)
        buf.writeVarInt(pos.z)
    }
}
