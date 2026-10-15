package dev.eymen.balancedshop;

import java.util.Map;
import java.util.Set;

/** Baseline prices in coins; recipes refine craftable items on the server. */
public final class PriceRules {
    private PriceRules() {}
    private static final Map<String, Long> EXACT = Map.ofEntries(
        Map.entry("diamond", 500L), Map.entry("diamond_block", 4500L),
        Map.entry("emerald", 180L), Map.entry("emerald_block", 1620L),
        Map.entry("iron_ingot", 30L), Map.entry("iron_nugget", 4L), Map.entry("iron_block", 270L),
        Map.entry("gold_ingot", 70L), Map.entry("gold_nugget", 8L), Map.entry("gold_block", 630L),
        Map.entry("copper_ingot", 12L), Map.entry("copper_block", 108L),
        Map.entry("netherite_ingot", 8000L), Map.entry("netherite_scrap", 1800L),
        Map.entry("netherite_block", 72000L), Map.entry("ancient_debris", 1800L),
        Map.entry("coal", 8L), Map.entry("charcoal", 8L), Map.entry("coal_block", 72L),
        Map.entry("redstone", 12L), Map.entry("redstone_block", 108L),
        Map.entry("lapis_lazuli", 16L), Map.entry("lapis_block", 144L),
        Map.entry("quartz", 18L), Map.entry("amethyst_shard", 24L),
        Map.entry("raw_iron", 24L), Map.entry("raw_gold", 56L), Map.entry("raw_copper", 10L),
        Map.entry("raw_iron_block", 216L), Map.entry("raw_gold_block", 504L), Map.entry("raw_copper_block", 90L),
        Map.entry("nether_star", 30000L), Map.entry("elytra", 50000L),
        Map.entry("dragon_egg", 100000L), Map.entry("dragon_head", 12000L),
        Map.entry("dragon_breath", 300L), Map.entry("totem_of_undying", 10000L),
        Map.entry("enchanted_golden_apple", 20000L), Map.entry("golden_apple", 600L),
        Map.entry("heart_of_the_sea", 10000L), Map.entry("trident", 8000L),
        Map.entry("heavy_core", 20000L), Map.entry("mace", 25000L),
        Map.entry("breeze_rod", 800L), Map.entry("blaze_rod", 160L),
        Map.entry("ender_pearl", 100L), Map.entry("ender_eye", 200L),
        Map.entry("ghast_tear", 250L), Map.entry("shulker_shell", 800L),
        Map.entry("echo_shard", 1200L), Map.entry("nautilus_shell", 250L),
        Map.entry("saddle", 300L), Map.entry("name_tag", 250L),
        Map.entry("experience_bottle", 180L), Map.entry("sponge", 800L),
        Map.entry("wet_sponge", 750L), Map.entry("obsidian", 60L),
        Map.entry("crying_obsidian", 100L), Map.entry("slime_ball", 35L),
        Map.entry("honey_bottle", 20L), Map.entry("honeycomb", 20L),
        Map.entry("leather", 20L), Map.entry("string", 8L), Map.entry("feather", 6L),
        Map.entry("bone", 8L), Map.entry("rotten_flesh", 2L), Map.entry("gunpowder", 25L),
        Map.entry("spider_eye", 12L), Map.entry("phantom_membrane", 120L),
        Map.entry("rabbit_foot", 80L), Map.entry("ink_sac", 10L), Map.entry("glow_ink_sac", 25L),
        Map.entry("flint", 6L), Map.entry("clay_ball", 3L), Map.entry("brick", 4L),
        Map.entry("wheat", 6L), Map.entry("carrot", 6L), Map.entry("potato", 6L),
        Map.entry("melon_slice", 2L), Map.entry("melon", 18L),
        Map.entry("beetroot", 6L), Map.entry("apple", 8L), Map.entry("egg", 5L),
        Map.entry("sugar_cane", 4L), Map.entry("bamboo", 2L), Map.entry("cactus", 4L),
        Map.entry("kelp", 2L), Map.entry("dried_kelp", 2L), Map.entry("sweet_berries", 4L),
        Map.entry("glow_berries", 8L), Map.entry("chorus_fruit", 20L),
        Map.entry("beef", 12L), Map.entry("porkchop", 12L), Map.entry("chicken", 10L),
        Map.entry("mutton", 10L), Map.entry("rabbit", 10L), Map.entry("cod", 8L),
        Map.entry("salmon", 10L), Map.entry("tropical_fish", 20L), Map.entry("pufferfish", 25L),
        Map.entry("dirt", 1L), Map.entry("cobblestone", 1L), Map.entry("stone", 2L),
        Map.entry("sand", 2L), Map.entry("red_sand", 3L), Map.entry("gravel", 2L),
        Map.entry("netherrack", 1L), Map.entry("end_stone", 8L), Map.entry("soul_sand", 8L),
        Map.entry("soul_soil", 8L), Map.entry("glass", 3L), Map.entry("deepslate", 3L),
        Map.entry("cobbled_deepslate", 2L), Map.entry("tuff", 3L), Map.entry("calcite", 5L),
        Map.entry("granite", 2L), Map.entry("diorite", 2L), Map.entry("andesite", 2L),
        Map.entry("blackstone", 4L), Map.entry("basalt", 3L), Map.entry("ice", 4L),
        Map.entry("packed_ice", 36L), Map.entry("blue_ice", 324L),
        Map.entry("snowball", 1L), Map.entry("snow_block", 4L), Map.entry("mud", 2L),
        Map.entry("clay", 12L), Map.entry("glowstone_dust", 20L),
        Map.entry("resin_clump", 15L), Map.entry("resin_brick", 18L),
        Map.entry("zinc_ingot", 24L), Map.entry("raw_zinc", 20L),
        Map.entry("brass_ingot", 60L), Map.entry("andesite_alloy", 20L),
        Map.entry("shaft", 10L), Map.entry("cogwheel", 25L), Map.entry("large_cogwheel", 50L),
        Map.entry("water_wheel", 180L), Map.entry("large_water_wheel", 400L),
        Map.entry("windmill_bearing", 800L), Map.entry("mechanical_bearing", 450L),
        Map.entry("mechanical_press", 600L), Map.entry("mechanical_mixer", 700L),
        Map.entry("millstone", 350L), Map.entry("crushing_wheel", 1800L),
        Map.entry("mechanical_crafter", 500L), Map.entry("mechanical_drill", 500L),
        Map.entry("mechanical_saw", 600L), Map.entry("deployer", 900L),
        Map.entry("encased_fan", 350L), Map.entry("mechanical_pump", 250L),
        Map.entry("fluid_pipe", 30L), Map.entry("fluid_tank", 500L),
        Map.entry("steam_engine", 2200L), Map.entry("steam_whistle", 300L),
        Map.entry("blaze_burner", 1000L), Map.entry("empty_blaze_burner", 120L),
        Map.entry("precision_mechanism", 1200L), Map.entry("electron_tube", 150L),
        Map.entry("rose_quartz", 80L), Map.entry("polished_rose_quartz", 100L),
        Map.entry("sturdy_sheet", 400L), Map.entry("track", 120L),
        Map.entry("train_controls", 2000L), Map.entry("train_station", 2500L),
        Map.entry("creative_motor", 1000000L),
        Map.entry("spawner", 25000L), Map.entry("trial_spawner", 125000L)
    );
    private static final Map<String,Long> EGGS=Map.ofEntries(
        Map.entry("pig",1000L),Map.entry("cow",1500L),Map.entry("chicken",1000L),Map.entry("sheep",1500L),
        Map.entry("rabbit",1250L),Map.entry("horse",2500L),Map.entry("donkey",2500L),Map.entry("mule",3000L),
        Map.entry("wolf",2500L),Map.entry("cat",2000L),Map.entry("fox",2500L),Map.entry("bee",3500L),
        Map.entry("zombie",5000L),Map.entry("husk",5500L),Map.entry("drowned",7500L),Map.entry("skeleton",7500L),
        Map.entry("stray",8000L),Map.entry("bogged",8000L),Map.entry("spider",5000L),Map.entry("cave_spider",6500L),
        Map.entry("creeper",10000L),Map.entry("slime",10000L),Map.entry("magma_cube",10000L),
        Map.entry("enderman",15000L),Map.entry("blaze",20000L),Map.entry("breeze",25000L),
        Map.entry("ghast",25000L),Map.entry("shulker",30000L),Map.entry("guardian",20000L),Map.entry("elder_guardian",75000L),
        Map.entry("villager",25000L),Map.entry("wandering_trader",15000L),Map.entry("witch",20000L),
        Map.entry("evoker",75000L),Map.entry("vindicator",15000L),Map.entry("pillager",10000L),Map.entry("ravager",50000L),
        Map.entry("iron_golem",25000L),Map.entry("snow_golem",2500L),Map.entry("piglin",15000L),
        Map.entry("piglin_brute",30000L),Map.entry("zombified_piglin",10000L),Map.entry("wither_skeleton",25000L),
        Map.entry("warden",250000L),Map.entry("wither",500000L),Map.entry("ender_dragon",750000L)
    );
    private static final Set<String> TECHNICAL = Set.of("air", "barrier", "bedrock", "structure_void",
        "structure_block", "jigsaw", "command_block", "command_block_minecart", "chain_command_block", "repeating_command_block",
        "light", "debug_stick", "knowledge_book", "vault",
        "reinforced_deepslate", "end_portal_frame", "budding_amethyst", "test_block", "test_instance_block");
    public static boolean restricted(String path) {
        return TECHNICAL.contains(path) || path.contains("creative")
            || path.startsWith("incomplete_") || path.equals("enchanted_book")
            || path.equals("potion") || path.equals("splash_potion") || path.equals("lingering_potion")
            || path.equals("tipped_arrow") || path.equals("filled_map");
    }
    public static boolean anchor(String namespace, String path) {
        if(namespace.equals("minecraft")&&(FarmEconomy.CROPS.containsKey(path)||FarmEconomy.BUNDLES.containsKey(path)))return true;
        return ((namespace.equals("minecraft") || namespace.equals("create")) && EXACT.containsKey(path))
            || restricted(path) || path.endsWith("_spawn_egg") || path.endsWith("_ore") || path.endsWith("_log") || path.endsWith("_stem")
            || path.endsWith("_leaves") || path.endsWith("_sapling") || path.endsWith("_propagule")
            || path.endsWith("_seeds") || path.endsWith("_wool") || path.endsWith("_dye")
            || path.endsWith("_smithing_template") || path.endsWith("_pottery_sherd");
    }
    public static long baseline(String namespace, String path, int durability, boolean block) {
        if(namespace.equals("minecraft")&&FarmEconomy.CROPS.containsKey(path))return FarmEconomy.CROPS.get(path)*100;
        Long exact = (namespace.equals("minecraft") || namespace.equals("create")) ? EXACT.get(path) : null;
        if (exact != null) return exact * 100;
        if(path.endsWith("_spawn_egg")) {
            String mob=path.substring(0,path.length()-"_spawn_egg".length());
            return (namespace.equals("minecraft")?EGGS.getOrDefault(mob,5000L):10000L)*100;
        }
        long coins = block ? 12 : 25;
        if (restricted(path)) coins = 1_000_000;
        else if (path.endsWith("_ore")) {
            coins = path.contains("diamond") ? 650 : path.contains("emerald") ? 260
                : path.contains("lapis") ? 200 : path.contains("copper") ? 80
                : path.contains("gold") ? 100 : path.contains("iron") ? 45 : 40;
        } else if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae")) coins = 8;
        else if (path.endsWith("_planks")) coins = 2;
        else if (path.endsWith("_sapling") || path.endsWith("_propagule")) coins = 12;
        else if (path.endsWith("_leaves")) coins = 2;
        else if (path.endsWith("_seeds")) coins = 3;
        else if (path.endsWith("_dye")) coins = 8;
        else if (path.endsWith("_wool")) coins = 15;
        else if (path.endsWith("_terracotta") || path.endsWith("_concrete")) coins = 10;
        else if (path.endsWith("_bucket")) coins = path.contains("lava") ? 160 : path.contains("water") ? 100 : 250;
        else if (path.endsWith("_smithing_template")) coins = 3500;
        else if (path.endsWith("_pottery_sherd")) coins = 250;
        else if (path.startsWith("music_disc_")) coins = 1500;
        else if (path.endsWith("_banner_pattern")) coins = 250;
        else if (durability > 0 || path.endsWith("_horse_armor") || path.endsWith("_nautilus_armor")) {
            long material = path.startsWith("netherite_") ? 10000 : path.startsWith("diamond_") ? 1500
                : path.startsWith("golden_") ? 300 : path.startsWith("iron_") ? 180
                : path.startsWith("copper_") ? 80 : path.startsWith("stone_") ? 20
                : path.startsWith("wooden_") ? 10 : path.startsWith("leather_") ? 100 : 250;
            int factor = path.endsWith("_chestplate") ? 3 : path.endsWith("_leggings") ? 3
                : path.endsWith("_helmet") || path.endsWith("_boots") || path.endsWith("_horse_armor") ? 2 : 1;
            coins = material * factor;
        } else if (!namespace.equals("minecraft")) {
            if (path.contains("ingot")) coins = 80;
            else if (path.contains("nugget")) coins = 10;
            else if (path.contains("plate") || path.contains("sheet")) coins = 100;
            else if (path.contains("machine") || path.contains("generator") || path.contains("motor")) coins = 1500;
            else if (path.contains("casing") || path.contains("mechanism")) coins = 300;
            else coins = block ? 80 : 60;
        }
        return coins * 100;
    }
}
