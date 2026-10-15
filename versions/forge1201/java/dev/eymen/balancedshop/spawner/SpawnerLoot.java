package dev.eymen.balancedshop.spawner;

import dev.eymen.balancedshop.BalancedShop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;

/** One ordinary, unenchanted player-kill loot roll every 80 loaded game ticks. */
public final class SpawnerLoot {
    public static final int INTERVAL=80;
    private SpawnerLoot(){}
    public static SpawnerStorage storage(SpawnerBlockEntity spawner){return ((StoredSpawner)spawner).balancedshop$storage();}
    public static void install(){MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,PlayerInteractEvent.RightClickBlock.class,SpawnerLoot::interact);}
    public static void tick(ServerLevel level,SpawnerBlockEntity spawner){
        SpawnerStorage store=storage(spawner);
        if(store.errorCooldown>0){store.errorCooldown--;return;}
        if(!store.pending.isEmpty()){store.flush();if(!store.pending.isEmpty())return;}
        if(store.full()||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))return;
        store.elapsed++;store.setChanged();if(store.elapsed<INTERVAL)return;store.elapsed=0;
        try{
            var data=spawner.getSpawner().save(new CompoundTag()).getCompound("SpawnData").getCompound("entity");
            store.pending.addAll(roll(level,data,Vec3.atCenterOf(spawner.getBlockPos()),0));store.flush();
        }catch(RuntimeException exception){
            // Faulty third-party loot tables must not crash the server or spam every tick.
            store.errorCooldown=1200;BalancedShop.LOG.error("Spawner loot failed at {} ({})",spawner.getBlockPos(),level.dimension().location(),exception);
        }
    }
    /** The entity is constructed for loot conditions only; never added to the level. */
    public static List<ItemStack> roll(ServerLevel level,CompoundTag data,Vec3 origin,long seed){
        var entity=EntityType.create(data,level).orElse(null);
        if(!(entity instanceof LivingEntity living))return List.of();
        entity.moveTo(origin.x,origin.y,origin.z,0,0);
        var killer=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("786b6b6e-335c-4f35-89e4-97ad4fcbbe34"),"[ShopSpawner]"));
        killer.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);killer.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,living)
            .withParameter(LootContextParams.ORIGIN,origin)
            .withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(killer))
            .withParameter(LootContextParams.KILLER_ENTITY,killer)
            .withParameter(LootContextParams.DIRECT_KILLER_ENTITY,killer)
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER,killer).withLuck(0).create(LootContextParamSets.ENTITY);
        return level.getServer().getLootData().getLootTable(living.getLootTable()).getRandomItems(params,seed);
    }
    private static void interact(PlayerInteractEvent.RightClickBlock e){
        if(e.getUseBlock()==Event.Result.DENY||e.getHand()!=InteractionHand.MAIN_HAND||e.getEntity().isShiftKeyDown())return;
        if(!(e.getLevel().getBlockEntity(e.getPos()) instanceof SpawnerBlockEntity spawner))return;
        if(!e.getEntity().mayBuild()||e.getEntity().isSpectator())return;
        if(e.getLevel() instanceof ServerLevel level&&e.getEntity() instanceof ServerPlayer player){
            if(!level.mayInteract(player,e.getPos())||!storage(spawner).stillValid(player))return;
            player.openMenu(new SimpleMenuProvider((id,inventory,p)->new StorageMenu(id,inventory,storage(spawner)),Component.literal("Üreteç Deposu · 4 saniye")));
        }
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
    public static void drop(SpawnerBlockEntity spawner){
        if(!(spawner.getLevel() instanceof ServerLevel level))return;
        var store=storage(spawner);var pos=spawner.getBlockPos();Containers.dropContents(level,pos,store);
        for(ItemStack item:store.pending)Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,item);
        store.clearContent();store.pending.clear();store.setChanged();
    }
    /** Native chest protocol/screen; insertion is rejected on the authoritative server. */
    private static final class OutputSlot extends Slot {
        OutputSlot(Container container,int index,int x,int y){super(container,index,x,y);}
        @Override public boolean mayPlace(ItemStack stack){return false;}
    }
    public static final class StorageMenu extends AbstractContainerMenu {
        private final SpawnerStorage store;
        public StorageMenu(int id,Inventory inventory,SpawnerStorage store){
            super(MenuType.GENERIC_9x6,id);this.store=store;
            for(int row=0;row<6;row++)for(int col=0;col<9;col++)addSlot(new OutputSlot(store,col+row*9,8+col*18,18+row*18));
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,140+row*18));
            for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,198));
        }
        @Override public boolean stillValid(Player player){return store.stillValid(player);}
        @Override public ItemStack quickMoveStack(Player player,int index){
            if(index<0||index>=54)return ItemStack.EMPTY;
            Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
            ItemStack stack=slot.getItem(),before=stack.copy();if(!moveItemStackTo(stack,54,slots.size(),true))return ItemStack.EMPTY;
            if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;
        }
    }
}
