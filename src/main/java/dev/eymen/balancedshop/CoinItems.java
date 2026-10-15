package dev.eymen.balancedshop;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import java.util.*;
public final class CoinItems {
    public record Denomination(String id,long value,Item item) {}
    public static final List<Denomination> ALL=List.of(register("copper_coin",1),register("silver_coin",100),register("gold_coin",10000),register("platinum_coin",1000000));
    private static Denomination register(String path,long cents){var id=Identifier.fromNamespaceAndPath("balancedshop",path);var item=Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));return new Denomination(id.toString(),cents,item);}
    public static void initialize() {}
    public static Denomination entry(int index){return index>=0&&index<ALL.size()?ALL.get(index):null;}
    public static long faceValue(Item item){return ALL.stream().filter(d->d.item()==item).mapToLong(Denomination::value).findFirst().orElse(0);}
    public static boolean plain(ItemStack stack){return !stack.isEmpty()&&faceValue(stack.getItem())>0&&ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem()));}
    public static long total(net.minecraft.world.entity.player.Inventory inventory){long sum=0;for(int i=0;i<36;i++){var stack=inventory.getItem(i);if(plain(stack)) sum=Math.addExact(sum,Math.multiplyExact(faceValue(stack.getItem()),(long)stack.getCount()));}return sum;}
    public static int count(net.minecraft.world.entity.player.Inventory inventory,int denomination){var coin=entry(denomination);if(coin==null) return 0;int count=0;for(int i=0;i<36;i++){var stack=inventory.getItem(i);if(stack.is(coin.item())&&plain(stack)) count+=stack.getCount();}return count;}
}
