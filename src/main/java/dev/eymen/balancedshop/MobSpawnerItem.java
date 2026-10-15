package dev.eymen.balancedshop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
/** A survival-placeable, server-issued spawner; no operator NBT permission is needed. */
public final class MobSpawnerItem extends BlockItem {
    public MobSpawnerItem(Item.Properties properties){super(Blocks.SPAWNER,properties);}
    public static String entityKey(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getString("shop_mob").orElse("");}
    public static ItemStack create(String id){ItemStack stack=new ItemStack(ModContent.SPAWNER_ITEM);CompoundTag tag=new CompoundTag();tag.putString("shop_mob",id);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
    @Override public Component getName(ItemStack stack){
        Identifier id=Identifier.tryParse(entityKey(stack));var type=id==null?null:BuiltInRegistries.ENTITY_TYPE.getValue(id);
        return type==null?Component.translatable("spawner.balancedshop.title"):Component.translatable("spawner.balancedshop.item",type.getDescription());
    }
    @Override protected boolean placeBlock(BlockPlaceContext context,BlockState state){
        Identifier id=Identifier.tryParse(entityKey(context.getItemInHand()));var type=id==null?null:BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if(type==null||!type.canSummon())return false;
        if(!super.placeBlock(context,state))return false;
        if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SpawnerBlockEntity spawner){
            configure(spawner,context.getLevel(),context.getClickedPos(),id.toString());
        }
        return true;
    }
    public static void configure(SpawnerBlockEntity spawner,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,String id){
        CompoundTag entity=new CompoundTag();entity.putString("id",id);
        CompoundTag rules=new CompoundTag(),light=new CompoundTag();light.putInt("min_inclusive",0);light.putInt("max_inclusive",15);
        rules.put("block_light_limit",light);rules.put("sky_light_limit",light.copy());
        CompoundTag spawn=new CompoundTag();spawn.put("entity",entity);spawn.put("custom_spawn_rules",rules);
        CompoundTag data=new CompoundTag();data.put("SpawnData",spawn);data.putShort("Delay",(short)100);data.putShort("MinSpawnDelay",(short)200);data.putShort("MaxSpawnDelay",(short)600);
        data.putShort("SpawnCount",(short)4);data.putShort("MaxNearbyEntities",(short)6);data.putShort("RequiredPlayerRange",(short)16);data.putShort("SpawnRange",(short)4);
        spawner.getSpawner().load(level,pos,data);spawner.setChanged();level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),3);
    }
}
