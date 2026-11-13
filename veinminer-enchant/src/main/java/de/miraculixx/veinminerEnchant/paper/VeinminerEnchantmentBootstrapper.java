package de.miraculixx.veinminerEnchant.paper;

import de.miraculixx.veinminer.EnchantmentPack;
import de.miraculixx.veinminer.data.VeinminerEnchantSettings;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class VeinminerEnchantmentBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        // Paper 1.21.1 has no datapack discovery, register via the registry API from plugins/Veinminer/enchantmentSettings.json
        LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();
        final VeinminerEnchantSettings config = EnchantmentPack.loadOrCreateSettings(Path.of("plugins", "Veinminer"));
        final var VEINMINE = TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(EnchantmentPack.ENCHANTMENT_ID));
        final String items = config.getSupportedItems();

        manager.registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> event.registry().register(
            VEINMINE,
            builder -> builder.description(Component.translatable("enchantment.veinminer_enchantment.veinminer", "Veinmine"))
                .supportedItems(items.startsWith("#")
                    ? event.getOrCreateTag(TagKey.create(RegistryKey.ITEM, Key.key(items.substring(1))))
                    : RegistrySet.keySet(RegistryKey.ITEM, TypedKey.create(RegistryKey.ITEM, Key.key(items))))
                .weight(Math.clamp(config.getWeight(), 1, 1024))
                .maxLevel(Math.clamp(config.getMaxLevel(), 1, 255))
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(config.getMinCost(), config.getMinCostPerLevel()))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(config.getMaxCost(), config.getMaxCostPerLevel()))
                .anvilCost(Math.max(config.getAnvilCost(), 0))
                .activeSlots(EquipmentSlotGroup.MAINHAND)
        )));

        manager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT), event -> {
            final PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
            Map.of(
                EnchantmentTagKeys.IN_ENCHANTING_TABLE, config.getInEnchantingTable(),
                EnchantmentTagKeys.TRADEABLE, config.getTradeable(),
                EnchantmentTagKeys.ON_RANDOM_LOOT, config.getLootable(),
                EnchantmentTagKeys.TREASURE, config.getTreasure()
            ).forEach((tag, enabled) -> {
                if (enabled) registrar.addToTag(tag, Set.of(VEINMINE));
            });
        });
    }

}
