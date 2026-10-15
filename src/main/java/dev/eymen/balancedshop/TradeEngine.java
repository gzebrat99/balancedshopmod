package dev.eymen.balancedshop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Runs on the server thread. Only the 36 storage slots participate in trades. */
public final class TradeEngine {
    private TradeEngine() {}
    public static String saleKey(ItemStack stack){String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();return stack.getItem() instanceof MobSpawnerItem?id+"@"+MobSpawnerItem.entityKey(stack):id;}
    public static String itemId(String saleKey){int at=saleKey.indexOf('@');return at<0?saleKey:saleKey.substring(0,at);}
    private static boolean matches(ItemStack stack,String id){return !stack.isEmpty()&&saleKey(stack).equals(id);}

    public static String trade(Catalogue catalogue, WalletStore wallets, Inventory inventory,
                               UUID player, String id, int quantity, boolean sell) throws IOException {
        if(quantity<1||quantity>64) return "invalid";
        return execute(catalogue,wallets,inventory,player,id,quantity,sell);
    }
    /** Zero means all plain copies of the selected item across the storage inventory. */
    public static String sell(Catalogue catalogue,WalletStore wallets,Inventory inventory,UUID player,String id,int quantity) throws IOException {
        if(quantity<0||quantity>2304) return "invalid";
        if(quantity==0) {
            var item=catalogue.item(itemId(id));if(item==null) return "restricted";
            ItemStack prototype=new ItemStack(item);
            for(int slot=0;slot<36;slot++) {
                var stack=inventory.getItem(slot);
                if(matches(stack,id)&&catalogue.sellValue(stack)>0) quantity=Math.addExact(quantity,stack.getCount());
            }
            if(quantity==0) return "items";
        }
        return execute(catalogue,wallets,inventory,player,id,quantity,true);
    }
    private static String execute(Catalogue catalogue,WalletStore wallets,Inventory inventory,UUID player,String id,int quantity,boolean sell) throws IOException {
        ShopPackets.Entry entry=catalogue.entry(itemId(id));
        if(entry==null || (sell ? !entry.sellable() : !entry.buyable())) return "restricted";
        ItemStack prototype=new ItemStack(catalogue.item(itemId(id)));
        if(prototype.isEmpty()) return "restricted";
        long balance=wallets.balance(player);
        long total=Math.multiplyExact(sell?entry.sell():entry.buy(),(long)quantity);
        if(sell){
            total=0;int remaining=quantity;
            for(int i=0;i<36&&remaining>0;i++){var stack=inventory.getItem(i);long price=catalogue.sellValue(stack);
                if(matches(stack,id)&&price>0){int take=Math.min(remaining,stack.getCount());total=Math.addExact(total,Math.multiplyExact(price,(long)take));remaining-=take;}}
            if(remaining>0) return "items";
        }
        if(!sell && balance<total) return "funds";
        if(sell && balance>Money.MAX-total) return "limit";
        int available=0;
        for(int slot=0;slot<36;slot++) {
            ItemStack stack=inventory.getItem(slot);
            if(sell) { if(matches(stack,id)&&catalogue.sellValue(stack)>0) available+=stack.getCount(); }
            else if(stack.isEmpty()) available+=Math.min(inventory.getMaxStackSize(),prototype.getMaxStackSize());
            else if(ItemStack.isSameItemSameComponents(stack,prototype))
                available+=Math.max(0,Math.min(inventory.getMaxStackSize(),stack.getMaxStackSize())-stack.getCount());
        }
        if(available<quantity) return sell?"items":"space";
        List<ItemStack> before=new ArrayList<>(36);
        for(int slot=0;slot<36;slot++) before.add(inventory.getItem(slot).copy());
        int left=quantity;
        // Merge purchases before taking empty slots. Never drop purchased items on the ground.
        for(int pass=0;pass<(sell?1:2) && left>0;pass++) for(int slot=0;slot<36 && left>0;slot++) {
            ItemStack stack=inventory.getItem(slot);
            if(sell && matches(stack,id)&&catalogue.sellValue(stack)>0) {
                int take=Math.min(left,stack.getCount()); stack.shrink(take); left-=take;
            } else if(!sell && pass==0 && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,prototype)) {
                int put=Math.min(left,Math.max(0,Math.min(inventory.getMaxStackSize(),stack.getMaxStackSize())-stack.getCount()));
                stack.grow(put); left-=put;
            } else if(!sell && pass==1 && stack.isEmpty()) {
                int put=Math.min(left,Math.min(inventory.getMaxStackSize(),prototype.getMaxStackSize()));
                inventory.setItem(slot,prototype.copyWithCount(put)); left-=put;
            }
        }
        try {
            if(left!=0) throw new IOException("Inventory changed during trade");
            wallets.set(player,sell?balance+total:balance-total);
        } catch(IOException | RuntimeException e) {
            for(int slot=0;slot<36;slot++) inventory.setItem(slot,before.get(slot));
            inventory.setChanged();
            throw e;
        }
        inventory.setChanged();
        return sell?"sold":"bought";
    }
}
