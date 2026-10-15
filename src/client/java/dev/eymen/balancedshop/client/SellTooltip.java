package dev.eymen.balancedshop.client;
import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class SellTooltip {
    private static final Map<String,PriceSync.Price> prices=new HashMap<>();private static long generation;
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(PriceSync.Packet.TYPE,(p,context)->{if(p.reset()){prices.clear();generation=p.generation();}if(generation==p.generation()) for(var entry:p.prices()) prices.put(entry.id(),entry);});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->prices.clear());
        ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->append(stack,lines));
    }
    public static void append(ItemStack stack,List<Component> lines){
        long face=CoinItems.faceValue(stack.getItem());if(face>0){lines.add(Component.translatable("coins.balancedshop.value",Money.format(face)).withColor(0xF0C878));lines.add(Component.translatable(CoinItems.plain(stack)?"coins.balancedshop.tooltip":"coins.balancedshop.protected").withColor(0xAABBD4));return;}
        if(stack.isEmpty()) return;var price=prices.get(stack.getItem() instanceof MobSpawnerItem?"spawner@"+MobSpawnerItem.entityKey(stack):BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());if(price==null) return;
        lines.add(Component.literal("────────────").withColor(0x374765));
        long value=ItemValues.sell(stack,price.buy(),price.sell(),price.sellable(),(id,level)->{var book=prices.get(id+"@"+level);return book==null?0:book.buy();});
        if(value<=0) lines.add(Component.translatable("tooltip.balancedshop.protected").withColor(0xE99B92));
        else{
            lines.add(Component.translatable("tooltip.balancedshop.sell",Money.format(value)).withColor(0x6DE5C5));
            if(stack.getCount()>1) lines.add(Component.translatable("tooltip.balancedshop.stack",stack.getCount(),Money.format(value*stack.getCount())).withColor(0xF4CD77));
        }
    }
}
