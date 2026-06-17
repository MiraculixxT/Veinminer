package de.miraculixx.veinminerClient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(
            method = "submitBlockOutline",
            at = @At("HEAD")
    )
    private void renderVeinminerHighlights(PoseStack poseStack,
                                           SubmitNodeCollector submitNodeCollector,
                                           LevelRenderState levelRenderState,
                                           CallbackInfo ci) {
        BlockHighlightingRenderer.INSTANCE.render(
                poseStack,
                submitNodeCollector,
                levelRenderState.cameraRenderState.pos
        );
    }

}
