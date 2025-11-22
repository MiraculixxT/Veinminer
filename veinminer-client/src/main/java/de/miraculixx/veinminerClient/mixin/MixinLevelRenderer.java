package de.miraculixx.veinminerClient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(
            method = "renderBlockOutline",
            at = @At("HEAD")
    )
    private void renderVeinminerHighlights(MultiBufferSource.BufferSource bufferSource,
                                           PoseStack poseStack,
                                           boolean isTransparentPass,
                                           LevelRenderState levelRenderState,
                                           CallbackInfo ci) {
        BlockHighlightingRenderer.INSTANCE.render(
                poseStack,
                bufferSource,
                levelRenderState.cameraRenderState.pos,
                isTransparentPass
        );
    }

}
