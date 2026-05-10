package de.miraculixx.veinminer.network

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf

interface PacketCodec<T> {
    fun read(buf: FriendlyByteBuf): T
    fun write(buf: FriendlyByteBuf, value: T)

    fun encode(value: T): ByteArray {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        try {
            write(buf, value)
            val out = ByteArray(buf.readableBytes())
            buf.readBytes(out)
            return out
        } finally {
            buf.release()
        }
    }

    fun decode(bytes: ByteArray): T {
        val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))
        try {
            return read(buf)
        } finally {
            buf.release()
        }
    }
}
