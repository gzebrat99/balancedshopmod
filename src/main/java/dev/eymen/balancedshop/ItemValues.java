package dev.eymen.balancedshop;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.function.ToLongBiFunction;
/** One valuation rule for sale, upgrade stakes and client tooltips. */
public final class ItemValues {
    private ItemValues() {}
    public static boolean sellableComponents(ItemStack stack){
        return !stack.isEmpty();
    }
    public static long sell(ItemStack stack,long buy,long sell,boolean enabled,ToLongBiFunction<String,Integer> bookPrices){
        if(!enabled||sell<=0||buy<=0||!sellableComponents(stack)) return 0;
        long value=buy;
        for(var entry:EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()){
            long price=bookPrices.applyAsLong(entry.getKey().unwrapKey().orElseThrow().identifier().toString(),entry.getIntValue());
            if(price<=0) continue;value=Math.addExact(value,price);
        }
        double durability=stack.isDamageableItem()?Math.max(0,1.0-(double)stack.getDamageValue()/stack.getMaxDamage()):1;
        if(durability<=0) return 0;
        return Math.max(1,(long)Math.floor(value*((double)sell/buy)*durability));
    }
}
