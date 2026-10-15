package dev.eymen.balancedshop;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Catalogue {
    public static final int PAGE_SIZE = 45;
    private final ShopConfig config;
    private final NavigableMap<String, ShopPackets.Entry> entries = new TreeMap<>();
    private final Map<String, Item> items = new HashMap<>();
    private final Map<String, Long> prices = new HashMap<>();
    private final Map<String, List<String>> tags = new HashMap<>();
    private final List<String> namespaces;
    private int recipeCount;
    private Map<String,Long> enchantmentPrices=Map.of();
    private SpawnerCatalogue spawners;
    public void attachSpawners(SpawnerCatalogue value){spawners=value;}
    public Collection<SpawnerCatalogue.Entry> spawners(){return spawners==null?List.of():spawners.all();}
    public void attachBooks(BookCatalogue books){Map<String,Long> map=new TreeMap<>();for(var e:books.all()) map.put(e.id()+"@"+e.level(),e.buy());enchantmentPrices=Map.copyOf(map);}
    public Map<String,Long> enchantmentPrices(){return enchantmentPrices;}
    public long sellValue(ItemStack stack){
        if(stack.isEmpty()) return 0;
        if(stack.getItem() instanceof MobSpawnerItem){var spawner=spawners==null?null:spawners.entry(MobSpawnerItem.entityKey(stack));return spawner==null||!config.sellingEnabled?0:spawner.sell();}
        var e=entry(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return e==null?0:ItemValues.sell(stack,e.buy(),e.sell(),e.sellable(),(id,level)->enchantmentPrices.getOrDefault(id+"@"+level,0L));
    }
    private record CostRecipe(String result, int output, List<List<String>> inputs, long fuel) {}

    public Catalogue(MinecraftServer server, ShopConfig config, Path export) throws IOException {
        this(server.getResourceManager(),config,export);
    }
    public Catalogue(ResourceManager resources, ShopConfig config, Path export) throws IOException {
        this.config=config;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            Identifier id=BuiltInRegistries.ITEM.getKey(item);
            String key=id.toString();
            long baseline=PriceRules.baseline(id.getNamespace(),id.getPath(),new ItemStack(item).getMaxDamage(),item instanceof BlockItem);
            long buy=Math.max(1,Math.min(Money.MAX_PRICE,Math.round(baseline * config.namespaceMultipliers.getOrDefault(id.getNamespace(),1.0))));
            if (config.prices.containsKey(key)) buy=config.prices.get(key).buy();
            items.put(key,item); prices.put(key,buy);
        }
        refineRecipes(resources);
        prices.replaceAll((id,price)->config.prices.containsKey(id)?price:
            Math.max(1,Math.min(Money.MAX_PRICE,Math.round(price*config.priceMultiplier))));
        FarmEconomy.BUNDLES.forEach((block,bundle)->{
            Long unit=prices.get("minecraft:"+bundle.material());
            if(unit!=null&&prices.containsKey("minecraft:"+block))prices.put("minecraft:"+block,FarmEconomy.bundlePrice(unit,bundle.count()));
        });
        for (var e:prices.entrySet()) {
            long face=CoinItems.faceValue(items.get(e.getKey()));
            if(face>0){entries.put(e.getKey(),new ShopPackets.Entry(e.getKey(),face,0,false,false));continue;}
            String path=Identifier.parse(e.getKey()).getPath();
            ShopConfig.Override override=config.prices.get(e.getKey());
            boolean hidden=hidden(e.getKey());
            boolean buyable=!hidden&&(override!=null ? override.buyable() : !PriceRules.restricted(path)||config.allowRestrictedItems);
            boolean sellable=config.sellingEnabled;
            long sell=config.sellRatio==0?0:Math.max(1,(long)Math.floor(e.getValue()*config.sellRatio));
            entries.put(e.getKey(),new ShopPackets.Entry(e.getKey(),e.getValue(),sell,buyable,sellable && sell>0));
        }
        FarmEconomy.BUNDLES.forEach((block,bundle)->{
            var unit=entries.get("minecraft:"+bundle.material());var packed=entries.get("minecraft:"+block);
            if(unit!=null&&packed!=null)entries.put(packed.id(),new ShopPackets.Entry(packed.id(),packed.buy(),Math.multiplyExact(unit.sell(),bundle.count()),packed.buyable(),packed.sellable()));
        });
        namespaces=entries.keySet().stream().map(s->Identifier.parse(s).getNamespace()).distinct().sorted().limit(256).toList();
        export(export);
    }
    public ShopPackets.Entry entry(String id) { return entries.get(id); }
    public Item item(String id) { return items.get(id); }
    public Collection<ShopPackets.Entry> all() { return Collections.unmodifiableCollection(entries.values()); }
    public int recipeCount() { return recipeCount; }
    public List<String> namespaces() { return namespaces; }
    public List<ShopPackets.Entry> search(String text, String namespace) {
        String query=text.strip().toLowerCase(Locale.ROOT);
        return entries.values().stream().filter(e->namespace.startsWith("@")?FarmEconomy.matches(namespace,e.id(),new ItemStack(items.get(e.id())).isDamageableItem(),items.get(e.id()) instanceof BlockItem):namespace.isEmpty()||e.id().startsWith(namespace+":"))
            .filter(e->!hidden(e.id())&&CoinItems.faceValue(items.get(e.id()))==0)
            .filter(e->query.isEmpty()||e.id().contains(query)||new ItemStack(items.get(e.id())).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
            .toList();
    }
    List<String> ingredient(JsonElement element) {
        if (element==null || element.isJsonNull()) return List.of();
        if (element.isJsonArray()) {
            LinkedHashSet<String> options=new LinkedHashSet<>();
            for(JsonElement option:element.getAsJsonArray()) options.addAll(ingredient(option));
            return List.copyOf(options);
        }
        if(element.isJsonObject()) {
            JsonObject o=element.getAsJsonObject();
            if(o.has("item")) return ingredient(o.get("item"));
            if(o.has("tag")) return ingredient(new JsonPrimitive("#"+o.get("tag").getAsString()));
            return List.of();
        }
        String name=element.getAsString();
        if(!name.startsWith("#")) return prices.containsKey(name)?List.of(name):List.of();
        return tags.computeIfAbsent(name, key -> {
            Identifier id=Identifier.tryParse(key.substring(1));
            if(id==null) return List.of();
            TagKey<Item> tag=TagKey.create(Registries.ITEM,id);
            return items.entrySet().stream().filter(e->new ItemStack(e.getValue()).is(tag)).map(Map.Entry::getKey).toList();
        });
    }
    private void refineRecipes(ResourceManager resourcesManager) {
        List<CostRecipe> recipes=new ArrayList<>();
        var resources=resourcesManager.listResources("recipe",id->id.getPath().endsWith(".json"));
        for(var resource:resources.entrySet()) {
            try(Reader reader=resource.getValue().openAsReader()) {
                JsonObject o=JsonParser.parseReader(reader).getAsJsonObject();
                String type=o.get("type").getAsString();
                boolean shaped=type.equals("minecraft:crafting_shaped"), shapeless=type.equals("minecraft:crafting_shapeless");
                boolean furnace=Set.of("minecraft:smelting","minecraft:blasting","minecraft:smoking","minecraft:campfire_cooking").contains(type);
                if(!shaped && !shapeless && !furnace && !type.equals("minecraft:stonecutting")) continue;
                JsonElement r=o.get("result"); if(r==null) continue;
                String result; int count;
                if(r.isJsonObject()) {
                    JsonObject resultObject=r.getAsJsonObject();
                    result=resultObject.get(resultObject.has("id")?"id":"item").getAsString();
                    count=resultObject.has("count")?resultObject.get("count").getAsInt():1;
                    if(resultObject.has("components")) continue;
                } else { result=r.getAsString(); count=o.has("count")?o.get("count").getAsInt():1; }
                if(!prices.containsKey(result)||count<1||count>64||config.prices.containsKey(result)) continue;
                List<List<String>> inputs=new ArrayList<>();
                if(shaped) {
                    JsonObject keys=o.getAsJsonObject("key");
                    for(JsonElement row:o.getAsJsonArray("pattern")) for(char c:row.getAsString().toCharArray())
                        if(c!=' ') inputs.add(ingredient(keys.get(String.valueOf(c))));
                } else if(shapeless) { for(JsonElement input:o.getAsJsonArray("ingredients")) inputs.add(ingredient(input)); }
                else inputs.add(ingredient(o.get("ingredient")));
                // Container returns (buckets, bottles, etc.) are deliberately counted at full cost.
                if(inputs.isEmpty()||inputs.stream().anyMatch(List::isEmpty)) continue;
                recipes.add(new CostRecipe(result,count,inputs,furnace?50:0));
            } catch(Exception e) { BalancedShop.LOG.debug("Skipped price derivation for {}: {}",resource.getKey(),e.toString()); }
        }
        recipeCount=recipes.size();
        Map<String,Long> fallback=new HashMap<>(prices);
        // Generic fallback prices must not undercut machinery/storage made from expensive ingredients.
        // Natural resources and curated items remain anchors; other recipe outputs start unresolved.
        for(CostRecipe r:recipes) {
            Identifier id=Identifier.parse(r.result);
            if(!PriceRules.anchor(id.getNamespace(),id.getPath())) prices.put(r.result,Money.MAX_PRICE);
        }
        relax(recipes);
        for(var entry:prices.entrySet()) if(entry.getValue()==Money.MAX_PRICE) entry.setValue(fallback.get(entry.getKey()));
        relax(recipes);
        BalancedShop.LOG.info("Priced {} registered items using {} recipes",prices.size(),recipeCount);
    }
    private void relax(List<CostRecipe> recipes) {
        for(int pass=0;pass<64;pass++) {
            boolean changed=false;
            for(CostRecipe r:recipes) {
                long cost=r.fuel;
                boolean unresolved=false;
                for(List<String> alternatives:r.inputs) {
                    long ingredient=alternatives.stream().mapToLong(prices::get).min().orElse(Money.MAX_PRICE);
                    if(ingredient==Money.MAX_PRICE) { unresolved=true;break; }
                    cost+=ingredient;
                }
                if(unresolved) continue;
                long derived=Math.max(1,Math.min(Money.MAX_PRICE,(cost+r.output-1)/r.output));
                Identifier target=Identifier.parse(r.result);
                if(PriceRules.anchor(target.getNamespace(),target.getPath()))continue;
                if(derived<prices.get(r.result)) { prices.put(r.result,derived); changed=true; }
            }
            if(!changed) break;
        }
    }
    private static boolean hidden(String id){String path=id.substring(id.indexOf(':')+1);return path.endsWith("_spawn_egg")||path.equals("spawner")||path.equals("trial_spawner")||path.equals("mob_spawner")||path.equals("uncrafting_table");}
    private void export(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder csv=new StringBuilder("item_id,buy_coins,sell_coins,buyable,sellable\n");
        for(ShopPackets.Entry e:entries.values()) csv.append(e.id()).append(',')
            .append(String.format(Locale.ROOT,"%d.%02d",e.buy()/100,e.buy()%100)).append(',')
            .append(String.format(Locale.ROOT,"%d.%02d",e.sell()/100,e.sell()%100)).append(',')
            .append(e.buyable()).append(',').append(e.sellable()).append('\n');
        Files.writeString(file,csv,StandardCharsets.UTF_8);
    }
}
