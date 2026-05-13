package de.miraculixx.veinminer.config

import kotlinx.serialization.modules.SerializersModule
import net.minecraft.resources.Identifier
import kotlin.io.path.Path

/**
 * Fabric & NeoForge exclusive - Paper carries its own impl
 */
object ConfigManager : BaseConfigManager<Identifier>(
    configDir = Path("config/Veinminer"),
    serializer = IdentifierConfigSerializer,
    jsonModule = SerializersModule {
        contextual(Identifier::class, ResourceLocationSerializer)
    }
)
