package dev.eymen.balancedshop.data;

import dev.eymen.balancedshop.FarmEconomy;
import net.minecraft.world.item.*;
import java.util.Set;

/** Shared category predicates run on the server; mod items participate via their actual Item properties. */
public final class MarketCategories {
    private static final Set<String> SPECIAL=Set.of("nether_star","beacon","elytra","dragon_egg","dragon_head","end_crystal","totem_of_undying","heart_of_the_sea","echo_shard","enchanted_golden_apple");
    private MarketCategories(){}
    public static boolean matches(String filter,String id,Item item){
        ItemStack stack=new ItemStack(item);String path=id.substring(id.indexOf(':')+1);
        if(filter.equals("@gear"))return stack.isDamageableItem()||item instanceof ArmorItem||item instanceof SwordItem||item instanceof ProjectileWeaponItem||item instanceof ShieldItem;
        if(filter.equals("@rare"))return stack.getRarity()!=Rarity.COMMON||SPECIAL.contains(path)||path.startsWith("netherite_");
        return FarmEconomy.matches(filter,id,stack.isDamageableItem(),item instanceof BlockItem);
    }
}
