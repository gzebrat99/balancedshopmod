package dev.eymen.balancedshop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.util.*;

public final class BookPurchase {
    private BookPurchase() {}
    public static String buy(BookCatalogue catalogue,WalletStore wallets,Inventory inventory,UUID player,String id,int level,int quantity) throws IOException {
        if(quantity<1||quantity>64) return "invalid";
        var entry=catalogue.entry(id,level);if(entry==null) return "invalid";
        long total=Money.total(entry.buy(),quantity),balance=wallets.balance(player);
        if(balance<total) return "funds";
        ItemStack prototype=entry.book();List<ItemStack> after=new ArrayList<>(36);
        for(int i=0;i<36;i++) after.add(inventory.getItem(i).copy());
        int left=quantity;
        for(int pass=0;pass<2&&left>0;pass++) for(int i=0;i<36&&left>0;i++) {
            var stack=after.get(i);int maximum=Math.min(inventory.getMaxStackSize(),prototype.getMaxStackSize());
            if(pass==0&&!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,prototype)) {
                int put=Math.min(left,Math.max(0,maximum-stack.getCount()));stack.grow(put);left-=put;
            }else if(pass==1&&stack.isEmpty()) {int put=Math.min(left,maximum);after.set(i,prototype.copyWithCount(put));left-=put;}
        }
        if(left>0) return "space";
        wallets.set(player,balance-total);
        for(int i=0;i<36;i++) inventory.setItem(i,after.get(i));
        inventory.setChanged();return "bought";
    }
}
