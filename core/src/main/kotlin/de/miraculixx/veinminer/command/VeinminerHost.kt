package de.miraculixx.veinminer.command

interface VeinminerHost {
    val versionLine: String
    var active: Boolean
    fun warn(msg: String)
}

object ActiveHost {
    @Volatile
    lateinit var host: VeinminerHost
}
