package dev.eymen.balancedshop.mixin.client;

import dev.eymen.balancedshop.placement.PlacementClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FabricPlacementMixin {
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void balancedshop$place(CallbackInfo ci){if(PlacementClient.tryUse())ci.cancel();}
}
