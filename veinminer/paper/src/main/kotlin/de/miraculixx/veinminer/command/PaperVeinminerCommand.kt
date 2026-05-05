package de.miraculixx.veinminer.command

import com.mojang.brigadier.tree.LiteralCommandNode
import de.miraculixx.veinminer.INSTANCE
import de.miraculixx.veinminer.VeinMinerEvent
import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.VeinminerCompatibility
import de.miraculixx.veinminer.config.ActiveConfig
import de.miraculixx.veinminer.config.ConfigManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.minecraft.commands.CommandBuildContext
import net.minecraft.server.MinecraftServer
import net.minecraft.world.flag.FeatureFlags
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.slf4j.Logger
import io.papermc.paper.command.brigadier.CommandSourceStack as PaperCommandSourceStack
import net.minecraft.commands.CommandSourceStack as NmsCommandSourceStack

private object PaperHost : VeinminerHost {
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

object PaperVeinminerCommand {
    fun register() {
        ActiveConfig.bridge = ConfigManager
        ActiveHost.host = PaperHost
        Permissions.install { src, node ->
            val sender: CommandSender? = when (src) {
                is NmsCommandSourceStack -> runCatching { src.bukkitSender }.getOrNull()
                is PaperCommandSourceStack -> src.sender
                else -> null
            }
            // OP and console always pass; otherwise check Bukkit perm node
            sender?.isOp == true || sender?.hasPermission(node) == true || (sender !is Player)
        }

        INSTANCE.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val server = MinecraftServer.getServer()
            val ctx = CommandBuildContext.simple(server.registryAccess(), FeatureFlags.REGISTRY.allFlags())
            val node = VeinminerCommand.build(ctx).build()
            @Suppress("UNCHECKED_CAST")
            event.registrar().register(
                node as LiteralCommandNode<PaperCommandSourceStack>,
                "Veinminer command",
            )
        }
    }
}
