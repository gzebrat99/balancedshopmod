package dev.eymen.balancedshop.verification;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.spawner.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real vanilla spawners, injected ticker, persisted NBT, loot resources and removal hook. */
public final class SpawnerLootVerification {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static CompoundTag mob(String id){var tag=new CompoundTag();tag.putString("id",id);return tag;}
    private static int count(List<ItemStack> items,Item item){return items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    public static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(180,90,180);level.getChunkAt(pos);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).forEach(Entity::discard);
        level.setBlockAndUpdate(pos,Blocks.SPAWNER.defaultBlockState());
        var spawner=(SpawnerBlockEntity)level.getBlockEntity(pos);MobSpawnerItem.configure(spawner,level,pos,"minecraft:iron_golem");
        var store=SpawnerLoot.storage(spawner);var box=new AABB(pos).inflate(12);
        int before=level.getEntitiesOfClass(Mob.class,box).size();
        for(int t=0;t<79;t++)SpawnerBlockEntity.serverTick(level,pos,spawner.getBlockState(),spawner);
        check(store.isEmpty(),"No loot before 80 game ticks");
        SpawnerBlockEntity.serverTick(level,pos,spawner.getBlockState(),spawner);
        check(store.countItem(Items.IRON_INGOT)>=3&&store.countItem(Items.IRON_INGOT)<=5,"One golem roll after exactly 80 ticks: 3-5 iron");
        check(store.countItem(Items.POPPY)<=2,"Golem poppy amount unchanged");
        check(level.getEntitiesOfClass(Mob.class,box).size()==before,"Native spawner ticker never adds mobs to the world");
        check(store.elapsed==0,"Timer resets after one roll");
        store.setItem(7,new ItemStack(Items.DIAMOND,17));store.elapsed=37;store.pending.add(new ItemStack(Items.EMERALD,9));
        CompoundTag saved=spawner.saveWithFullMetadata();
        var restored=(SpawnerBlockEntity)BlockEntity.loadStatic(pos,spawner.getBlockState(),saved);var reloaded=SpawnerLoot.storage(restored);
        check(reloaded.countItem(Items.DIAMOND)==17,"Storage survives native block entity NBT reload");
        check(reloaded.getItem(7).is(Items.DIAMOND),"Exact storage slot positions survive NBT reload");
        check(reloaded.elapsed==37,"Partial interval survives NBT reload");
        check(count(reloaded.pending,Items.EMERALD)==9,"Unfinished batch survives NBT reload");
        store.pending.clear();for(int i=0;i<54;i++)store.setItem(i,new ItemStack(Items.COBBLESTONE,64));store.elapsed=0;
        for(int t=0;t<160;t++)SpawnerBlockEntity.serverTick(level,pos,spawner.getBlockState(),spawner);
        check(store.countItem(Items.COBBLESTONE)==3456&&store.elapsed==0,"Full storage pauses without consuming or overwriting items");
        store.pending.add(new ItemStack(Items.IRON_INGOT,5));store.flush();check(count(store.pending,Items.IRON_INGOT)==5,"Overflow is retained when full");
        store.setItem(0,ItemStack.EMPTY);store.flush();check(store.countItem(Items.IRON_INGOT)==5&&store.pending.isEmpty(),"Freeing space resumes the pending batch without a reroll");
        for(int t=0;t<79;t++)SpawnerBlockEntity.serverTick(level,pos,spawner.getBlockState(),spawner);
        check(store.countItem(Items.IRON_INGOT)==5,"Resumed production still waits a complete interval");
        SpawnerBlockEntity.serverTick(level,pos,spawner.getBlockState(),spawner);
        check(store.countItem(Items.IRON_INGOT)>=8&&store.countItem(Items.IRON_INGOT)<=10,"Resumed spawner produces exactly one additional golem roll");
        check(!store.canPlaceItem(0,new ItemStack(Items.DIAMOND)),"Output storage rejects external insertion");
        var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LootVerifier"));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var menu=new SpawnerLoot.StorageMenu(1,player.getInventory(),store);
        check(menu.stillValid(player),"Nearby player can use storage");
        menu.setCarried(new ItemStack(Items.DIAMOND,3));int ironBefore=store.countItem(Items.IRON_INGOT);
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        check(menu.getCarried().getCount()==3&&store.countItem(Items.IRON_INGOT)==ironBefore,"Cursor swap cannot insert into output slots");menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(0,new ItemStack(Items.DIAMOND,5));menu.quickMoveStack(player,81);
        check(player.getInventory().countItem(Items.DIAMOND)==5&&store.countItem(Items.DIAMOND)==0,"Player shift transfer cannot insert into storage");
        player.setPos(pos.getX()+20,pos.getY(),pos.getZ());check(!menu.stillValid(player),"Walking away invalidates the container");
        var gameRule=level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);boolean old=gameRule.get();gameRule.set(false,server);store.elapsed=0;
        SpawnerBlockEntity.serverTick(level,pos,spawner.getBlockState(),spawner);check(store.elapsed==0,"doMobLoot=false pauses generation");gameRule.set(old,server);
        int rareBlaze=0;var seeds=net.minecraft.util.RandomSource.create(918273645L);
        for(int seed=1;seed<=64;seed++){
            var loot=SpawnerLoot.roll(level,mob("minecraft:iron_golem"),Vec3.atCenterOf(pos),seed);
            check(count(loot,Items.IRON_INGOT)>=3&&count(loot,Items.IRON_INGOT)<=5&&count(loot,Items.POPPY)<=2,"Vanilla golem bounds seed "+seed);
            rareBlaze+=count(SpawnerLoot.roll(level,mob("minecraft:blaze"),Vec3.atCenterOf(pos),seeds.nextLong()),Items.BLAZE_ROD);
        }
        check(rareBlaze>0&&rareBlaze<64,"Player-kill-only blaze rods retain chance, not guaranteed");
        check(SpawnerLoot.roll(level,mob("minecraft:villager"),Vec3.atCenterOf(pos),42).isEmpty(),"No invented villager loot");
        var red=mob("minecraft:sheep");red.putByte("Color",(byte)14);
        check(count(SpawnerLoot.roll(level,red,Vec3.atCenterOf(pos),42),Items.RED_WOOL)==1,"Dynamic sheep-color loot table uses entity NBT");
        check(SpawnerLoot.roll(level,mob("invalid:missing"),Vec3.atCenterOf(pos),42).isEmpty(),"Unknown entity ID safely produces nothing");
        var catalogue=new SpawnerCatalogue(server,new ShopConfig());
        for(var entry:catalogue.all()){
            SpawnerLoot.roll(level,mob(entry.id()),Vec3.atCenterOf(pos),42);check(true,"Registered mob loot can be evaluated: "+entry.id());
        }
        store.clearContent();store.setItem(0,new ItemStack(Items.DIAMOND,11));store.pending.add(new ItemStack(Items.EMERALD,4));
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var dropped=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        check(dropped.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==11,"Breaking the block drops stored loot exactly once");
        check(dropped.stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum()==4,"Breaking also drops the pending batch");
        check(store.isEmpty()&&store.pending.isEmpty(),"Removed inventory is emptied against duplicate drops");
        dropped.forEach(Entity::discard);
        return checks;
    }
}
