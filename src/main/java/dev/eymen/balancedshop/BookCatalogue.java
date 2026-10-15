package dev.eymen.balancedshop;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class BookCatalogue {
    public static final int PAGE_SIZE=6;
    public record Entry(String id,int level,long buy,ItemStack book) {}
    private final Map<String,Entry> entries=new TreeMap<>();
    private static final Map<String,Long> BASE=Map.ofEntries(
        Map.entry("mending",10000L),Map.entry("unbreaking",1000L),Map.entry("efficiency",1250L),
        Map.entry("fortune",2500L),Map.entry("silk_touch",6000L),Map.entry("sharpness",1000L),
        Map.entry("protection",1500L),Map.entry("looting",2500L),Map.entry("sweeping_edge",1500L),
        Map.entry("soul_speed",3000L),Map.entry("swift_sneak",2500L),Map.entry("wind_burst",5000L),
        Map.entry("infinity",6000L),Map.entry("binding_curse",250L),Map.entry("vanishing_curse",250L)
    );
    public BookCatalogue(MinecraftServer server,ShopConfig config,Path export) throws IOException {
        var registry=server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for(var holder:registry.listElements().toList()) {
            String id=holder.key().identifier().toString();var enchantment=holder.value();
            for(int level=enchantment.getMinLevel();level<=Math.min(255,enchantment.getMaxLevel());level++) {
                Identifier key=holder.key().identifier();
                long price=config.enchantmentPrices.getOrDefault(id+"@"+level,
                    price(key.getNamespace(),key.getPath(),level,enchantment.getWeight(),enchantment.getMinCost(level),
                        holder.is(EnchantmentTags.TREASURE),holder.is(EnchantmentTags.CURSE),config.priceMultiplier,
                        config.namespaceMultipliers.getOrDefault(key.getNamespace(),1.0)));
                entries.put(id+"@"+level,new Entry(id,level,price,EnchantmentHelper.createBook(new EnchantmentInstance(holder,level))));
            }
        }
        Files.createDirectories(export.getParent());
        List<String> lines=new ArrayList<>();lines.add("enchantment_id,level,buy_coins");
        entries.values().forEach(e->lines.add(e.id+","+e.level+","+String.format(Locale.ROOT,"%.2f",e.buy/100.0)));
        Files.write(export,lines,StandardCharsets.UTF_8);
        BalancedShop.LOG.info("Priced {} enchanted book variants",entries.size());
    }
    public static long price(String namespace,String path,int level,int weight,int minCost,boolean treasure,boolean curse,double multiplier,double namespaceMultiplier) {
        if(level<1||level>255||!Double.isFinite(multiplier)||multiplier<=0||!Double.isFinite(namespaceMultiplier)||namespaceMultiplier<=0) throw new IllegalArgumentException("Invalid book price parameters");
        long base=namespace.equals("minecraft")&&BASE.containsKey(path)?BASE.get(path):curse?250:
            Math.max(500,Math.max(0,(long)minCost+10)*50+10000/Math.max(1,weight))*(treasure?2:1);
        return Math.max(1,Math.min(Money.MAX_PRICE,Math.round(base*100.0*Math.pow(2,level-1)*multiplier*namespaceMultiplier)));
    }
    public Entry entry(String id,int level){return entries.get(id+"@"+level);}
    public Collection<Entry> all(){return Collections.unmodifiableCollection(entries.values());}
    public List<Entry> search(String query){
        String text=query.strip().toLowerCase(Locale.ROOT);
        return entries.values().stream().filter(e->e.id.toLowerCase(Locale.ROOT).contains(text)||name(e.book).getString().toLowerCase(Locale.ROOT).contains(text)).toList();
    }
    public static net.minecraft.network.chat.Component name(ItemStack book){
        return book.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY).entrySet().stream()
            .findFirst().map(e->Enchantment.getFullname(e.getKey(),e.getIntValue())).orElse(book.getHoverName());
    }
}
