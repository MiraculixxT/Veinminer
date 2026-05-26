package de.miraculixx.veinminer;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class VeinminerBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLogger().warn("Paper 1.21.1 does not expose datapack discovery during bootstrap; bundled c: tags are not auto-registered.");
    }

}
