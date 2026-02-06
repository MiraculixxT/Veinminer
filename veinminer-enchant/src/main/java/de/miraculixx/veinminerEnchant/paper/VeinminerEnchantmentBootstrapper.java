package de.miraculixx.veinminerEnchant.paper;

import com.google.gson.Gson;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class VeinminerEnchantmentBootstrapper implements PluginBootstrap {
    private static final Gson GSON = new Gson()
            .newBuilder()
            .setPrettyPrinting()
            .create();

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();

        // Add new enchantment
        final var VEINMINE = TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("veinminer-enchantment:veinminer"));

        final var config = loadConfig(context);

        manager.registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> event.registry().register(
            VEINMINE,
            builder -> builder.description(Component.translatable("enchantment.veinmine", "Veinmine"))
                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_MINING))
                .weight(1)
                .maxLevel(1)
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(config.getMinCost(), 0))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(config.getMaxCost(), 0))
                .anvilCost(config.getAnvilCost())
                .activeSlots(EquipmentSlotGroup.MAINHAND)
        )));

        // Add enchantment to enchanting table and co
        manager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT), event -> {
            final PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
            registrar.addToTag(EnchantmentTagKeys.TRADEABLE, Set.of(VEINMINE));
            registrar.addToTag(EnchantmentTagKeys.NON_TREASURE, Set.of(VEINMINE));
            registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, Set.of(VEINMINE));
        });
    }

    private VeinminerEnchantmentSettings loadConfig(BootstrapContext context) {
        Path path = context.getDataDirectory().resolve("config.json");
        File file = path.toFile();

        if (!file.exists()) {
            try {
                String json = GSON.toJson(new VeinminerEnchantmentSettings());
                Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                return new VeinminerEnchantmentSettings();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return GSON.fromJson(Files.readString(path), VeinminerEnchantmentSettings.class);
        } catch (IOException e) {
            return new VeinminerEnchantmentSettings();
        }
    }
}
