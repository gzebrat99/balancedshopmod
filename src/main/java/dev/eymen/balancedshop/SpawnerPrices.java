package dev.eymen.balancedshop;
import java.util.*;
/** Infinite production is priced separately from one-off mob drops. Values are coins. */
public final class SpawnerPrices {
    private SpawnerPrices() {}
    private static final Map<String,Long> PRICES=Map.ofEntries(
        Map.entry("pig",50_000L),Map.entry("chicken",65_000L),Map.entry("cow",80_000L),Map.entry("sheep",100_000L),
        Map.entry("rabbit",80_000L),Map.entry("zombie",125_000L),Map.entry("skeleton",250_000L),Map.entry("spider",150_000L),
        Map.entry("creeper",500_000L),Map.entry("slime",650_000L),Map.entry("magma_cube",650_000L),
        Map.entry("enderman",750_000L),Map.entry("blaze",750_000L),Map.entry("breeze",1_000_000L),
        Map.entry("guardian",1_000_000L),Map.entry("shulker",1_250_000L),Map.entry("witch",750_000L),
        Map.entry("villager",1_500_000L),Map.entry("wandering_trader",600_000L),Map.entry("iron_golem",2_500_000L),
        Map.entry("snow_golem",100_000L),Map.entry("wither_skeleton",2_000_000L),Map.entry("evoker",5_000_000L),
        Map.entry("elder_guardian",4_000_000L),Map.entry("warden",7_500_000L),Map.entry("wither",15_000_000L),Map.entry("ender_dragon",25_000_000L));
    public static long buy(String id){String path=id.substring(id.indexOf(':')+1);return Math.multiplyExact(PRICES.getOrDefault(path,id.startsWith("minecraft:")?250_000L:750_000L),100);}
    public static long sell(String id,double ratio){return (long)Math.floor(buy(id)*ratio);}
    public static String tier(String id){long cost=buy(id)/100;return cost>=5_000_000?"boss":cost>=1_000_000?"industrial":cost>=250_000?"advanced":"starter";}
}
