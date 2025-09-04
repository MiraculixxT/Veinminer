package de.miraculixx.veinminer;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class VeinminerBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.DATAPACK_DISCOVERY.newHandler(event -> {
                    try {
                        URI uri = Objects.requireNonNull(getClass().getResource("/veinminer_dp")).toURI();
                        event.registrar().discoverPack(uri, "provided-c-tags");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to discover c: tags datapack", e);
                    }
                })
        );

    }

}
