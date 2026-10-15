package dev.eymen.balancedshop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import dev.eymen.balancedshop.legacy.LegacyValues;
import java.util.function.ToLongBiFunction;
public final class ItemValues {
    public static boolean sellableComponents(ItemStack stack){return !stack.isEmpty();}
    public static long sell(ItemStack stack,long buy,long sell,boolean enabled,ToLongBiFunction<String,Integer> bookPrices){
        if(!enabled||sell<=0||buy<=0||!sellableComponents(stack))return 0;
        long value=buy;
        for(var entry:EnchantmentHelper.getEnchantments(stack).entrySet()){
            long price=bookPrices.applyAsLong(BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey()).toString(),entry.getValue());
            if(price<=0)continue;value=Math.addExact(value,price);
        }
        double durability=stack.isDamageableItem()?Math.max(0,1.0-(double)stack.getDamageValue()/stack.getMaxDamage()):1;
        if(durability<=0)return 0;return Math.max(1,(long)Math.floor(value*((double)sell/buy)*durability));
    }
}
