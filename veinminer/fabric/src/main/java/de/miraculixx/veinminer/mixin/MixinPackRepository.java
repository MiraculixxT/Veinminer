package de.miraculixx.veinminer.mixin;

import de.miraculixx.veinminer.config.EnchantmentPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public class MixinPackRepository {

    @Shadow
    @Final
    @Mutable
    public Set<RepositorySource> sources;

    // Fabric API has no way to inject generated datapacks, add source to every server data repository
    @Inject(method = "<init>", at = @At("RETURN"))
    private void veinminerAddEnchantmentPack(RepositorySource[] repositorySources, CallbackInfo ci) {
        if (sources.stream().noneMatch(s -> s instanceof ServerPacksSource)) return;
        sources = new LinkedHashSet<>(sources);
        sources.add(EnchantmentPackSource.INSTANCE);
    }
}
