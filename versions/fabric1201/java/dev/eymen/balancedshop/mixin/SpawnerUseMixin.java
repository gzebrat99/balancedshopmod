package dev.eymen.balancedshop.mixin;

import dev.eymen.balancedshop.spawner.SpawnerLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla block use happens only after Fabric UseBlockCallback accepted the interaction. */
@Mixin(BlockBehaviour.class)
public abstract class SpawnerUseMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void balancedshop$use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir){
        if(level.getBlockEntity(pos) instanceof SpawnerBlockEntity){
            var result=SpawnerLoot.interact(player,level,hand,hit);if(result!=InteractionResult.PASS)cir.setReturnValue(result);
        }
    }
}
