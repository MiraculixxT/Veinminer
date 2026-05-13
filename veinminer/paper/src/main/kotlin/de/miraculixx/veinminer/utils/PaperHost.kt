package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.INSTANCE
import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.VeinminerCompatibility
import de.miraculixx.veinminer.command.VeinminerHost
import org.slf4j.Logger

object PaperHost : VeinminerHost {
    override val versionVeinminer: String = INSTANCE.pluginMeta.version
    override val versionMinecraft: String = INSTANCE.server.minecraftVersion
    override val platform: String = VeinminerCompatibility.platform.name
    override val logger: Logger = Veinminer.LOGGER

    override var active: Boolean
        get() = VeinMinerEvent.enabled
        set(value) {
            VeinMinerEvent.enabled = value
        }
}
