package dev.eymen.balancedshop.legacy;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
/** Preserve unknown NBT, lore, containers and mod data when checking sale eligibility. */
public final class LegacyValues {
    public static ItemStack clean(ItemStack source,boolean enchanting){
        ItemStack clean=source.copyWithCount(1);
        if(clean.getTag()==null)return clean;
        var tag=clean.getTag().copy();
        if(enchanting||source.isDamageableItem())for(String key:Set.of("Damage","Enchantments","RepairCost"))tag.remove(key);
        if(enchanting){tag.remove("StoredEnchantments");removeName(tag);}
        if(tag.isEmpty())clean.setTag(null);else clean.setTag(tag);
        return clean;
    }
    private static void removeName(net.minecraft.nbt.CompoundTag tag){
        if(tag.contains("display",10)){var display=tag.getCompound("display").copy();display.remove("Name");if(display.isEmpty())tag.remove("display");else tag.put("display",display);}
    }
    public static boolean eligible(ItemStack stack,boolean enchanting){return !stack.isEmpty()&&ItemStack.isSameItemSameTags(clean(stack,enchanting),clean(new ItemStack(stack.getItem()),enchanting));}
    public static ItemStack withoutName(ItemStack stack){var copy=stack.copy();if(copy.getTag()!=null){removeName(copy.getTag());if(copy.getTag().isEmpty())copy.setTag(null);}return copy;}
}
