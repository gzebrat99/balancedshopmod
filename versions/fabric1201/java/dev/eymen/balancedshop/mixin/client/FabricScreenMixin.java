package dev.eymen.balancedshop.mixin.client;

import dev.eymen.balancedshop.screen.SpawnerStorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Replace only our storage screen; ordinary vanilla/mod chest screens keep their own renderer. */
@Mixin(Minecraft.class)
public abstract class FabricScreenMixin {
    @ModifyVariable(method="setScreen",at=@At("HEAD"),argsOnly=true)
    private Screen balancedshop$storage(Screen screen){
        var client=(Minecraft)(Object)this;
        if(client.player!=null&&screen instanceof ContainerScreen chest&&!(chest instanceof SpawnerStorageScreen)&&chest.getTitle().getString().equals("Üreteç Deposu · 4 saniye"))
            return new SpawnerStorageScreen(chest.getMenu(),client.player.getInventory(),chest.getTitle());
        return screen;
    }
}
