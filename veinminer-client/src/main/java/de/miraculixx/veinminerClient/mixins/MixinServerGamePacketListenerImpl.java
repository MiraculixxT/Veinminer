package de.miraculixx.veinminerClient.mixins;

import de.miraculixx.veinminerClient.VeinminerClient;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {

    @Inject(
        method = "handleCustomPayload",
        at = @At("HEAD"),
        order = 1
    )
    private void onHandleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        VeinminerClient.Companion.getLOGGER().info("Received Custom Payload");
        VeinminerClient.Companion.getLOGGER().info(packet.payload().toString());
    }
}
