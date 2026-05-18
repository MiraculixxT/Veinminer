package de.miraculixx.veinminerClient.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.miraculixx.veinminerClient.KeyBindManager;
import de.miraculixx.veinminerClient.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(
            method = "onScroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void veinminerOnScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!KeyBindManager.INSTANCE.isPressed()) return;
        if (!NetworkManager.INSTANCE.isVeinminerActive()) return;
        if (vertical == 0.0) return;
        Window w = Minecraft.getInstance().getWindow();
        boolean shift = InputConstants.isKeyDown(w, InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(w, InputConstants.KEY_RSHIFT);
        KeyBindManager.INSTANCE.queueScroll(vertical > 0 ? 1 : -1, shift);
        ci.cancel();
    }
}
