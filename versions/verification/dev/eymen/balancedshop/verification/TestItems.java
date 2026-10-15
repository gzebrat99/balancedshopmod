package dev.eymen.balancedshop.verification;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
public final class TestItems {
    public static ItemStack named(ItemStack item,String name){item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal(name));return item;}
}
