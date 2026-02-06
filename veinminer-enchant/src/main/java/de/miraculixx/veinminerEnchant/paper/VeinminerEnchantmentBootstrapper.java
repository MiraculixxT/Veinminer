package de.miraculixx.veinminerEnchant.paper;

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
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class VeinminerEnchantmentBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();

        // Add new enchantment
        final var VEINMINE = TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("veinminer-enchantment:veinminer"));

        // Config
        final var config = VeinminerEnchantmentSettings.Companion.get();
        TagKey<ItemType> itemTag;
        if (config.getPickaxeOnly()) itemTag = ItemTypeTagKeys.PICKAXES;
        else itemTag = ItemTypeTagKeys.ENCHANTABLE_MINING;

        manager.registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> event.registry().register(
            VEINMINE,
            builder -> builder.description(Component.translatable("enchantment.veinmine", "Veinmine"))
                .supportedItems(event.getOrCreateTag(itemTag))
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

}
