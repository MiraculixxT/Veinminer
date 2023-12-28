package de.miraculixx.template.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
class MixinExample {

    @Inject(
        at = @At("HEAD"),
        method = "explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"
    )
    private void onExplode(Entity entity, double d, double e, double f, float g, Level.ExplosionInteraction explosionInteraction, CallbackInfoReturnable<Explosion> cir) {
        System.out.println("Boom!");
    }
}