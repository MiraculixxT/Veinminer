package de.miraculixx.veinminer.config

import kotlinx.serialization.modules.SerializersModule
import org.bukkit.NamespacedKey
import kotlin.io.path.Path

object PaperConfigManager : BaseConfigManager<NamespacedKey>(
    configDir = Path("plugins/Veinminer"),
    serializer = PaperConfigSerializer,
    jsonModule = SerializersModule {
        contextual(NamespacedKey::class, NamespacedKeySerializer)
    },
) {
    init {
        reload(true)
    }
}
