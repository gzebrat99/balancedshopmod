package dev.eymen.balancedshop.verification;

import dev.eymen.balancedshop.placement.AreaPlacement;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import java.util.*;

/** Actual native Forge placement and rollback, not a mock inventory implementation. */
public final class PlacementVerification {
    private static int checks;
    private static BlockPos blocked,denied;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static int run(ServerPlayer player){
        checks=0;var level=player.serverLevel();BlockPos center=new BlockPos(120,80,120),anchor=center.below();
        for(Direction face:Direction.values())for(int size=3;size<=10;size++){
            var plane=AreaPlacement.plane(center,face,size);
            check(plane.size()==size*size&&new HashSet<>(plane).size()==size*size,"Unique plane "+face+" "+size);
            check(plane.get(0).equals(center)&&plane.stream().allMatch(p->switch(face.getAxis()){case X->p.getX()==center.getX();case Y->p.getY()==center.getY();case Z->p.getZ()==center.getZ();}),"Plane orientation and center "+face+" "+size);
        }
        check(!AreaPlacement.validSize(2)&&!AreaPlacement.validSize(11)&&AreaPlacement.plane(center,Direction.UP,Integer.MAX_VALUE).isEmpty(),"Packet size is bounded before allocation");
        try{Class.forName("dev.eymen.balancedshop.VeinMiningService");throw new AssertionError("Mining service still exists");}catch(ClassNotFoundException expected){checks++;}
        try{Class.forName("dev.eymen.balancedshop.VeinPackets");throw new AssertionError("Mining packets still exist");}catch(ClassNotFoundException expected){checks++;}
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e)->{if(e.getPos().equals(blocked))e.setCanceled(true);});
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e)->{if(e.getPos().equals(denied))e.setUseItem(Event.Result.DENY);});
        player.setGameMode(GameType.SURVIVAL);player.setPos(120.5,82,120.5);player.getInventory().selected=0;
        var hit=new BlockHitResult(new Vec3(120.5,80,120.5),Direction.UP,anchor,false);
        try{
            clear(player,center);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,64));player.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,36));
            int count=AreaPlacement.place(player,hit,10);
            check(count==100,"10x10 places 100 blocks using multiple inventory stacks");
            check(AreaPlacement.plane(center,Direction.UP,10).stream().allMatch(p->level.getBlockState(p).is(Blocks.COBBLESTONE)),"Whole plane placed in world");
            check(player.getInventory().countItem(Items.COBBLESTONE)==0,"Exact material consumption, no free blocks");
            clear(player,center);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,4));
            check(AreaPlacement.place(player,hit,3)==4&&player.getMainHandItem().isEmpty(),"Insufficient materials produce a partial plane");
            clear(player,center);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,9));blocked=center;
            check(AreaPlacement.place(player,hit,3)==8,"Forge EntityPlaceEvent cancellation skips protected cell");
            check(level.getBlockState(center).isAir()&&player.getInventory().countItem(Items.COBBLESTONE)==1,"Protection rolls back block and restores material");blocked=null;
            clear(player,center);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,9));denied=center;
            check(AreaPlacement.place(player,hit,3)==8&&level.getBlockState(center).isAir(),"RightClickBlock use-item denial is respected");denied=null;
            clear(player,center);level.setBlockAndUpdate(center.offset(1,0,0),Blocks.DIAMOND_BLOCK.defaultBlockState());player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,9));
            check(AreaPlacement.place(player,hit,3)==8&&level.getBlockState(center.offset(1,0,0)).is(Blocks.DIAMOND_BLOCK),"Existing solid blocks are never replaced or broken");
            clear(player,center);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,1));var named=new ItemStack(Items.COBBLESTONE,8);named.setHoverName(net.minecraft.network.chat.Component.literal("different"));player.getInventory().setItem(1,named);
            check(AreaPlacement.place(player,hit,3)==1&&player.getInventory().getItem(1).getCount()==8,"Refill does not consume stacks with different NBT");
            clear(player,center);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,9));player.setPos(150,80,150);
            check(AreaPlacement.place(player,hit,3)==0&&player.getMainHandItem().getCount()==9,"Out-of-reach anchor rejected without consuming items");player.setPos(120.5,82,120.5);
            check(AreaPlacement.place(player,hit,0)==0&&AreaPlacement.place(player,hit,11)==0,"Invalid dimensions change nothing");
            player.setGameMode(GameType.SPECTATOR);check(AreaPlacement.place(player,hit,3)==0,"Spectator placement rejected");
            player.setGameMode(GameType.CREATIVE);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,1));
            check(AreaPlacement.place(player,hit,3)==9&&player.getMainHandItem().getCount()==1,"Creative placement preserves held item");
            clear(player,center);player.setGameMode(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            check(AreaPlacement.place(player,hit,3)==0,"Non-block items cannot trigger area placement");
        }finally{blocked=null;denied=null;clear(player,center);player.setGameMode(GameType.SURVIVAL);}
        return checks;
    }
    private static void clear(ServerPlayer player,BlockPos center){
        for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,ItemStack.EMPTY);
        for(var pos:AreaPlacement.plane(center,Direction.UP,10)){
            player.serverLevel().getChunkAt(pos);player.serverLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());player.serverLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        }
    }
}
