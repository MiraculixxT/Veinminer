package de.miraculixx.veinminer.command

import org.slf4j.Logger

interface VeinminerHost {
    val versionVeinminer: String
    val versionMinecraft: String
    val platform: String
    val logger: Logger
    var active: Boolean
}

object ActiveHost {
    @Volatile
    lateinit var host: VeinminerHost
}
