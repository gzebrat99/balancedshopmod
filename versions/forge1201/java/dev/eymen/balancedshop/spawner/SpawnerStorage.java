package dev.eymen.balancedshop.spawner;

import net.minecraft.nbt.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import java.util.*;

/** Saved in the spawner's own chunk NBT, including an unfinished overflow batch. */
public final class SpawnerStorage extends SimpleContainer {
    public final SpawnerBlockEntity owner;
    public final List<ItemStack> pending=new ArrayList<>();
    public int elapsed;
    public int errorCooldown;
    public SpawnerStorage(SpawnerBlockEntity owner){super(54);this.owner=owner;}
    @Override public void setChanged(){super.setChanged();owner.setChanged();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return false;}
    @Override public boolean stillValid(Player player){return !owner.isRemoved()&&owner.getLevel()!=null&&owner.getLevel().getBlockEntity(owner.getBlockPos())==owner&&player.distanceToSqr(owner.getBlockPos().getX()+.5,owner.getBlockPos().getY()+.5,owner.getBlockPos().getZ()+.5)<=64;}
    public boolean full(){for(int i=0;i<getContainerSize();i++)if(getItem(i).isEmpty()||getItem(i).getCount()<Math.min(getMaxStackSize(),getItem(i).getMaxStackSize()))return false;return true;}
    public void flush(){
        for(int i=0;i<pending.size();){ItemStack left=addItem(pending.get(i));if(left.isEmpty())pending.remove(i);else{pending.set(i,left);i++;}}
        setChanged();
    }
    public CompoundTag save(){
        CompoundTag tag=new CompoundTag();ListTag items=new ListTag();
        for(int i=0;i<getContainerSize();i++)if(!getItem(i).isEmpty()){CompoundTag item=getItem(i).save(new CompoundTag());item.putInt("Slot",i);items.add(item);}tag.put("Items",items);tag.putInt("Elapsed",elapsed);
        ListTag overflow=new ListTag();for(ItemStack item:pending)if(!item.isEmpty())overflow.add(item.save(new CompoundTag()));tag.put("Pending",overflow);return tag;
    }
    public void load(CompoundTag tag){
        clearContent();pending.clear();
        for(Tag entry:tag.getList("Items",Tag.TAG_COMPOUND)){var item=(CompoundTag)entry;int slot=item.getInt("Slot");if(slot>=0&&slot<getContainerSize())setItem(slot,ItemStack.of(item));}
        elapsed=Math.max(0,Math.min(79,tag.getInt("Elapsed")));
        for(Tag entry:tag.getList("Pending",Tag.TAG_COMPOUND)){ItemStack item=ItemStack.of((CompoundTag)entry);if(!item.isEmpty())pending.add(item);}
    }
}
