package dev.eymen.balancedshop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class SpawnerCatalogue {
    public static final int PAGE_SIZE=6;
    public record Entry(String id,long buy,long sell,ItemStack stack) {}
    private final NavigableMap<String,Entry> entries=new TreeMap<>();
    public SpawnerCatalogue(MinecraftServer server,ShopConfig config){
        for(var type:BuiltInRegistries.ENTITY_TYPE){
            if(!type.canSummon())continue;
            try{if(!(type.create(server.overworld()) instanceof Mob))continue;
                String id=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();long buy=config.spawnerPrices.getOrDefault(id,SpawnerPrices.buy(id));entries.put(id,new Entry(id,buy,(long)Math.floor(buy*config.sellRatio),MobSpawnerItem.create(id)));
            }catch(Exception e){BalancedShop.LOG.warn("Spawner entity could not be inspected: {}",BuiltInRegistries.ENTITY_TYPE.getKey(type),e);}
        }
    }
    public void export(java.nio.file.Path path) throws java.io.IOException{
        java.nio.file.Files.createDirectories(path.getParent());StringBuilder csv=new StringBuilder("mob_id,buy_coins,sell_coins\n");
        for(var e:entries.values())csv.append(e.id()).append(',').append(String.format(Locale.ROOT,"%d.%02d",e.buy()/100,e.buy()%100)).append(',').append(String.format(Locale.ROOT,"%d.%02d",e.sell()/100,e.sell()%100)).append('\n');
        java.nio.file.Files.writeString(path,csv,java.nio.charset.StandardCharsets.UTF_8);
    }
    public Entry entry(String id){return entries.get(id);}
    public Collection<Entry> all(){return Collections.unmodifiableCollection(entries.values());}
    public List<Entry> search(String text){String q=text.strip().toLowerCase(Locale.ROOT);return entries.values().stream().filter(e->q.isEmpty()||e.id.contains(q)||e.stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).sorted(Comparator.comparingLong(Entry::buy).thenComparing(Entry::id)).toList();}
}
