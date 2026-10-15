package dev.eymen.balancedshop.placement;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

/** Clients send only a size; target, permissions, materials and placement are server-owned. */
public final class AreaPlacement {
    private static final String VERSION="1";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("balancedshop","placement"),()->VERSION,VERSION::equals,VERSION::equals);
    private static final Map<UUID,Integer> LAST_USE=new HashMap<>();
    public record Request(int size){}
    private AreaPlacement(){}
    public static boolean validSize(int size){return size>=3&&size<=10;}
    public static void install(){
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->b.writeVarInt(p.size())).decoder(b->new Request(b.readVarInt())).consumerMainThread((p,c)->handle(c.get().getSender(),p.size())).add();
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->LAST_USE.remove(e.getEntity().getUUID()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e)->LAST_USE.clear());
    }
    public static void handle(ServerPlayer player,int size){
        if(player==null||!validSize(size)||!player.isAlive()||player.isSpectator()||!player.mayBuild()||player.containerMenu!=player.inventoryMenu)return;
        int now=player.server.getTickCount();Integer last=LAST_USE.get(player.getUUID());
        if(last!=null&&now-last<8)return;
        LAST_USE.put(player.getUUID(),now);
        double reach=Math.min(6,player.getAttributeValue(ForgeMod.BLOCK_REACH.get())+(player.isCreative()?0.5:0));
        Vec3 eye=player.getEyePosition();
        var hit=player.level().clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(reach)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK)return;
        int placed=place(player,hit,size);
        player.displayClientMessage(Component.literal(placed+" blok yerleştirildi · "+size+"×"+size),true);
    }
    /** Even sizes extend one extra cell in each positive plane axis, with the aimed cell first. */
    public static List<BlockPos> plane(BlockPos center,Direction face,int size){
        if(!validSize(size))return List.of();
        List<BlockPos> result=new ArrayList<>(size*size);int low=-(size-1)/2;
        for(int a=low;a<low+size;a++)for(int b=low;b<low+size;b++)result.add(switch(face.getAxis()){
            case Y->center.offset(a,0,b);case Z->center.offset(a,b,0);case X->center.offset(0,b,a);
        });
        result.sort(Comparator.comparingDouble(p->p.distSqr(center)));return List.copyOf(result);
    }
    public static int place(ServerPlayer player,BlockHitResult hit,int size){
        if(!validSize(size)||!player.isAlive()||player.isSpectator()||!player.mayBuild()||player.containerMenu!=player.inventoryMenu)return 0;
        var level=player.serverLevel();BlockPos anchor=hit.getBlockPos();
        // Anchor must be a real, reachable block. No client coordinates or remote chunk loads.
        double reach=Math.min(6,player.getAttributeValue(ForgeMod.BLOCK_REACH.get())+(player.isCreative()?0.5:0));
        if(!level.hasChunkAt(anchor)||level.getBlockState(anchor).isAir()||player.getEyePosition().distanceToSqr(hit.getLocation())>(reach+0.01)*(reach+0.01)||!level.mayInteract(player,anchor))return 0;
        ItemStack material=player.getMainHandItem().copy();if(!(material.getItem() instanceof BlockItem))return 0;
        var initial=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        BlockPos center=initial.getClickedPos();int placed=0;
        for(BlockPos target:plane(center,hit.getDirection(),size)){
            if(!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(target)||level.isOutsideBuildHeight(target)||!level.mayInteract(player,target))continue;
            // Refill only identical stacks (including NBT/capabilities); never create materials.
            if(player.getMainHandItem().isEmpty()&&!refill(player,material))break;
            ItemStack held=player.getMainHandItem();if(!ItemStack.isSameItemSameTags(held,material))break;
            var targetHit=new BlockHitResult(Vec3.atCenterOf(target),hit.getDirection(),target,false);
            var context=new UseOnContext(player,InteractionHand.MAIN_HAND,targetHit);
            var placement=new BlockPlaceContext(context);
            if(!placement.getClickedPos().equals(target)||!level.getBlockState(target).canBeReplaced(placement))continue;
            var permission=ForgeHooks.onRightClickBlock(player,InteractionHand.MAIN_HAND,target,targetHit);
            if(permission.isCanceled()||permission.getUseItem()==Event.Result.DENY||permission.getUseBlock()==Event.Result.DENY)continue;
            // ItemStack.useOn invokes Forge's snapshot/EntityPlaceEvent pipeline, including rollback.
            // Calling BlockItem.place directly would bypass that protection and is intentionally avoided.
            if(held.useOn(context).consumesAction())placed++;
        }
        player.getInventory().setChanged();player.inventoryMenu.sendAllDataToRemote();return placed;
    }
    private static boolean refill(ServerPlayer player,ItemStack material){
        for(int slot=0;slot<36;slot++){
            if(slot==player.getInventory().selected)continue;
            ItemStack stack=player.getInventory().getItem(slot);
            if(!stack.isEmpty()&&ItemStack.isSameItemSameTags(stack,material)){
                player.setItemInHand(InteractionHand.MAIN_HAND,player.getInventory().removeItem(slot,stack.getMaxStackSize()));return true;
            }
        }
        return false;
    }
}
