package dev.eymen.balancedshop.legacy;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class LegacyMenu extends AbstractContainerMenu {
    public LegacyMenu(){super(null,-1);}
    @Override public boolean stillValid(Player player){return true;}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
