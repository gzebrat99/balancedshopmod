package dev.eymen.balancedshop.mixin;

import dev.eymen.balancedshop.spawner.SpawnerLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class SpawnerRemovalMixin {
    @Inject(method="onRemove",at=@At("HEAD"))
    private void balancedshop$drop(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving,CallbackInfo ci){
        if(state.getBlock() instanceof SpawnerBlock&&!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)SpawnerLoot.drop(spawner);
    }
}
