package dev.eymen.balancedshop.legacy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegisterEvent;
public final class NativeRegistrations {
    public static final ThreadLocal<RegisterEvent> EVENT=new ThreadLocal<>();
    public static Item registerItem(ResourceLocation id,Item item){EVENT.get().register(Registries.ITEM,h->h.register(id,item));return item;}
    public static Block registerBlock(ResourceLocation id,Block block){EVENT.get().register(Registries.BLOCK,h->h.register(id,block));return block;}
}
