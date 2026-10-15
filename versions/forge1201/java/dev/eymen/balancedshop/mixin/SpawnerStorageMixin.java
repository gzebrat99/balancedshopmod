package dev.eymen.balancedshop.mixin;

import dev.eymen.balancedshop.spawner.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerStorageMixin implements StoredSpawner {
    @Unique private SpawnerStorage balancedshop$store;
    @Override public SpawnerStorage balancedshop$storage(){if(balancedshop$store==null)balancedshop$store=new SpawnerStorage((SpawnerBlockEntity)(Object)this);return balancedshop$store;}
    @Inject(method="serverTick",at=@At("HEAD"),cancellable=true)
    private static void balancedshop$tick(Level level,BlockPos pos,BlockState state,SpawnerBlockEntity spawner,CallbackInfo ci){
        if(level instanceof ServerLevel server)SpawnerLoot.tick(server,spawner);ci.cancel();
    }
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void balancedshop$save(CompoundTag tag,CallbackInfo ci){if(balancedshop$store!=null)tag.put("BalancedShopStorage",balancedshop$store.save());}
    @Inject(method="load",at=@At("TAIL"))
    private void balancedshop$load(CompoundTag tag,CallbackInfo ci){balancedshop$storage().load(tag.getCompound("BalancedShopStorage"));}
}
