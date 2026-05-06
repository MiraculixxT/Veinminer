package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.command.VeinminerHost
import net.minecraft.DetectedVersion
import org.slf4j.Logger

object NeoForgeHost : VeinminerHost {
    override val versionVeinminer: String = Veinminer.INSTANCE.modInfo.version.toString()
    override val versionMinecraft: String = DetectedVersion.tryDetectVersion().name()
    override val platform: String = "NeoForge"
    override val logger: Logger = Veinminer.LOGGER

    override var active: Boolean
        get() = Veinminer.active
        set(value) {
            Veinminer.active = value
        }
}
