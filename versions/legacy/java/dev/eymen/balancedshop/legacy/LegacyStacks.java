package dev.eymen.balancedshop.legacy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
public final class LegacyStacks {
    public static ItemStack decode(FriendlyByteBuf buffer){return buffer.readItem();}
    public static void encode(FriendlyByteBuf buffer,ItemStack stack){buffer.writeItem(stack);}
}
