package dev.eymen.balancedshop.mixin;
import dev.eymen.balancedshop.CreateTraderService;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractVillager.class)
public abstract class CreateVillagerMixin {
    @Inject(method="getOffers",at=@At("HEAD"),cancellable=true)
    private void balancedshop$createOnly(CallbackInfoReturnable<MerchantOffers> callback){
        if((Object)this instanceof Villager v&&!v.level().isClientSide()&&CreateTraderService.replaces(v)) callback.setReturnValue(new MerchantOffers());
    }
}
