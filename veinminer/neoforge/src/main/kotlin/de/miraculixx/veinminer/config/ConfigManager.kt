package de.miraculixx.veinminer.config

import kotlinx.serialization.modules.SerializersModule
import net.minecraft.resources.Identifier
import kotlin.io.path.Path

object ConfigManager : BaseConfigManager<Identifier>(
    configDir = Path("config/Veinminer"),
    serializer = NeoForgeConfigSerializer,
    jsonModule = SerializersModule {
        contextual(Identifier::class, ResourceLocationSerializer)
    }
)
