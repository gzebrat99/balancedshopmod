package dev.eymen.balancedshop.localization;

import com.google.gson.*;
import dev.eymen.balancedshop.BalancedShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Explicit Turkish names work on dedicated servers too; Minecraft's global language is untouched. */
public final class TurkishText {
    private static volatile Map<String,String> translations;
    private static final Map<String,String> SEARCH=new ConcurrentHashMap<>();
    private static final Locale TR=Locale.forLanguageTag("tr-TR");
    private TurkishText(){}
    private static Map<String,String> dictionary(){
        var ready=translations;if(ready!=null)return ready;
        synchronized(TurkishText.class){
            if(translations!=null)return translations;
            Map<String,String> names=new HashMap<>();
            resource(names,"/assets/balancedshop/lang/search_tr_tr.json");
            resource(names,"/assets/balancedshop/lang/tr_tr.json");
            Set<String> namespaces=new TreeSet<>();BuiltInRegistries.ITEM.keySet().forEach(id->namespaces.add(id.getNamespace()));BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id->namespaces.add(id.getNamespace()));
            // Each mod owns its asset paths. No internet, client-only classes or locale-dependent I18n.
            for(String namespace:namespaces)if(!namespace.equals("minecraft")&&!namespace.equals("balancedshop")){
                ModList.get().getModContainerById(namespace).ifPresent(mod->{
                    Path path=mod.getModInfo().getOwningFile().getFile().findResource("assets",namespace,"lang","tr_tr.json");
                    if(Files.isRegularFile(path))try(var reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){read(names,reader);}catch(Exception e){BalancedShop.LOG.warn("Turkish names could not be loaded for {}",namespace,e);}
                });
            }
            // Common Create terms also cover versions with incomplete Turkish language packs.
            names.putIfAbsent("block.create.shaft","Şaft");names.putIfAbsent("item.create.shaft","Şaft");
            names.putIfAbsent("item.create.andesite_alloy","Andezit Alaşımı");names.putIfAbsent("block.create.cogwheel","Dişli");names.putIfAbsent("block.create.large_cogwheel","Büyük Dişli");
            translations=Map.copyOf(names);return translations;
        }
    }
    private static void resource(Map<String,String> names,String path){
        try(var stream=TurkishText.class.getResourceAsStream(path)){
            if(stream==null)throw new IllegalStateException("Missing Turkish resource "+path);
            try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){read(names,reader);}
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static void read(Map<String,String> names,Reader reader){
        JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e->{if(e.getValue().isJsonPrimitive()&&e.getValue().getAsJsonPrimitive().isString())names.put(e.getKey(),e.getValue().getAsString());});
    }
    public static Component component(Component original){
        MutableComponent result;
        if(original.getContents() instanceof TranslatableContents translated){
            Object[] args=translated.getArgs().clone();
            for(int i=0;i<args.length;i++)if(args[i] instanceof Component c)args[i]=component(c);
            String pattern=dictionary().get(translated.getKey());
            if(pattern==null)result=Component.translatable(translated.getKey(),args);
            else {
                Object[] plain=args.clone();for(int i=0;i<plain.length;i++)if(plain[i] instanceof Component c)plain[i]=c.getString();
                try{result=Component.literal(String.format(TR,pattern,plain));}catch(IllegalFormatException e){result=Component.literal(pattern);}
            }
        }else result=original.plainCopy();
        result.setStyle(original.getStyle());for(var sibling:original.getSiblings())result.append(component(sibling));return result;
    }
    public static Component name(ItemStack stack){return component(stack.getHoverName());}
    public static Component book(ItemStack stack){
        return EnchantmentHelper.getEnchantments(stack).entrySet().stream().findFirst().map(e->component(e.getKey().getFullname(e.getValue()))).orElseGet(()->name(stack));
    }
    public static String fold(String text){
        return Normalizer.normalize(text.toLowerCase(TR),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('ı','i').replace("blogu","blok").replaceAll("\\s+"," ").strip();
    }
    public static boolean matches(String query,String... values){
        String q=fold(query);if(q.isEmpty())return true;
        String haystack=fold(String.join(" ",values));
        if(haystack.replace(" ","").contains(q.replace(" ","")))return true;
        return Arrays.stream(q.split(" ")).allMatch(haystack::contains);
    }
    public static boolean itemMatches(String id,ItemStack stack,String query){
        String names=SEARCH.computeIfAbsent(id,key->id+" "+name(new ItemStack(stack.getItem())).getString()+" "+new ItemStack(stack.getItem()).getHoverName().getString()+" "+aliases(id));
        return matches(query,names,stack.hasCustomHoverName()?stack.getHoverName().getString():"");
    }
    public static boolean bookMatches(String id,int level,ItemStack stack,String query){return matches(query,id,String.valueOf(level),book(stack).getString(),aliases(id));}
    public static boolean spawnerMatches(String id,ItemStack stack,String query){return matches(query,id,name(stack).getString());}
    private static String aliases(String id){return switch(id){
        case "minecraft:mending"->"onarım tamir";case "minecraft:unbreaking"->"kırılmazlık sağlamlık";
        case "create:shaft"->"mil şaft";case "create:andesite_alloy"->"andezit alaşım";default->"";
    };}
}
