package dev.eymen.balancedshop;

import java.util.*;

/** Curated renewable resources and lossless storage conversions, in base coins. */
public final class FarmEconomy {
    private FarmEconomy() {}
    public record Bundle(String material,int count) {}
    public static long bundlePrice(long unit,int count){
        if(unit<1||count<1||unit>Money.MAX_PRICE/count)throw new IllegalArgumentException("Compressed price exceeds transaction limit");
        return Math.multiplyExact(unit,count);
    }
    public static final Map<String,Long> CROPS=Map.ofEntries(
        Map.entry("wheat",16L),Map.entry("carrot",12L),Map.entry("potato",12L),Map.entry("beetroot",16L),
        Map.entry("pumpkin",48L),Map.entry("melon",36L),Map.entry("melon_slice",4L),
        Map.entry("wheat_seeds",4L),Map.entry("pumpkin_seeds",4L),Map.entry("melon_seeds",4L),Map.entry("beetroot_seeds",4L),
        Map.entry("torchflower_seeds",8L),Map.entry("pitcher_pod",8L),Map.entry("cocoa_beans",12L),
        Map.entry("sugar_cane",8L),Map.entry("bamboo",2L),Map.entry("cactus",10L),Map.entry("kelp",4L),
        Map.entry("dried_kelp",4L),Map.entry("sweet_berries",10L),Map.entry("glow_berries",16L),
        Map.entry("chorus_fruit",30L),Map.entry("nether_wart",16L),Map.entry("brown_mushroom",12L),
        Map.entry("red_mushroom",12L),Map.entry("apple",16L),Map.entry("egg",12L));
    public static final Map<String,Bundle> BUNDLES=Map.ofEntries(
        Map.entry("diamond_block",new Bundle("diamond",9)),Map.entry("emerald_block",new Bundle("emerald",9)),
        Map.entry("iron_block",new Bundle("iron_ingot",9)),Map.entry("gold_block",new Bundle("gold_ingot",9)),
        Map.entry("copper_block",new Bundle("copper_ingot",9)),Map.entry("netherite_block",new Bundle("netherite_ingot",9)),
        Map.entry("coal_block",new Bundle("coal",9)),Map.entry("redstone_block",new Bundle("redstone",9)),
        Map.entry("lapis_block",new Bundle("lapis_lazuli",9)),Map.entry("raw_iron_block",new Bundle("raw_iron",9)),
        Map.entry("raw_gold_block",new Bundle("raw_gold",9)),Map.entry("raw_copper_block",new Bundle("raw_copper",9)),
        Map.entry("hay_block",new Bundle("wheat",9)),Map.entry("dried_kelp_block",new Bundle("dried_kelp",9)),
        Map.entry("quartz_block",new Bundle("quartz",4)),Map.entry("amethyst_block",new Bundle("amethyst_shard",4)),
        Map.entry("bone_block",new Bundle("bone_meal",9)),Map.entry("slime_block",new Bundle("slime_ball",9)),
        Map.entry("honey_block",new Bundle("honey_bottle",4)));
    public static boolean farm(String path) {
        return CROPS.containsKey(path)||path.endsWith("_seeds")||path.endsWith("_sapling")||path.endsWith("_leaves")
            ||path.endsWith("_log")||path.endsWith("_wood")||path.endsWith("_wool")||path.contains("mushroom")
            ||Set.of("bone","bone_meal","rotten_flesh","string","gunpowder","slime_ball","honeycomb","honey_bottle",
                "leather","feather","beef","porkchop","chicken","mutton","rabbit","cod","salmon","ink_sac","glow_ink_sac").contains(path);
    }
    public static boolean managed(String id) {
        if(!id.startsWith("minecraft:"))return false;
        String path=id.substring(10);
        return CROPS.containsKey(path)||BUNDLES.containsKey(path)||BUNDLES.values().stream().anyMatch(b->b.material.equals(path));
    }
    public static boolean matches(String filter,String id,boolean tool,boolean block) {
        String path=id.substring(id.indexOf(':')+1);
        return switch(filter) {
            case "@farm"->farm(path);
            case "@mine"->BUNDLES.containsKey(path)||path.contains("ingot")||path.contains("nugget")||path.endsWith("_ore")||path.startsWith("raw_")||Set.of("diamond","emerald","coal","redstone","lapis_lazuli","quartz","amethyst_shard").contains(path);
            case "@tools"->tool;
            case "@blocks"->block;
            default->true;
        };
    }
}
