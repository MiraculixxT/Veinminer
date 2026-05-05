package de.miraculixx.veinminer.network

import kotlinx.serialization.json.Json

object Codec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    inline fun <reified T> encode(value: T): ByteArray =
        json.encodeToString(value).encodeToByteArray()

    inline fun <reified T> decode(bytes: ByteArray): T =
        json.decodeFromString(bytes.decodeToString())
}
