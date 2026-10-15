package dev.eymen.balancedshop;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.util.*;
/** Fixed denomination values; components and prices cannot mint currency. */
public final class CoinEngine {
    public static final int MAX_QUANTITY=2304;
    private CoinEngine() {}
    static List<ItemStack> copy(Inventory inventory){List<ItemStack> after=new ArrayList<>(36);for(int i=0;i<36;i++) after.add(inventory.getItem(i).copy());return after;}
    static void commit(Inventory inventory,List<ItemStack> after){for(int i=0;i<36;i++) inventory.setItem(i,after.get(i));inventory.setChanged();}
    static boolean add(List<ItemStack> after,Inventory inventory,ItemStack item,int quantity){
        int maximum=Math.min(inventory.getMaxStackSize(),item.getMaxStackSize()),left=quantity;
        for(int pass=0;pass<2&&left>0;pass++) for(int i=0;i<36&&left>0;i++){
            var stack=after.get(i);
            if(pass==0&&!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,item)){int put=Math.min(left,Math.max(0,maximum-stack.getCount()));stack.grow(put);left-=put;}
            else if(pass==1&&stack.isEmpty()){int put=Math.min(left,maximum);after.set(i,item.copyWithCount(put));left-=put;}
        }return left==0;
    }
    public static String withdraw(WalletStore wallets,Inventory inventory,UUID player,int denomination,int quantity) throws IOException {
        var coin=CoinItems.entry(denomination);if(coin==null||quantity<1||quantity>MAX_QUANTITY) return "invalid";
        long amount=Math.multiplyExact(coin.value(),(long)quantity),balance=wallets.balance(player);if(balance<amount) return "funds";
        var after=copy(inventory);if(!add(after,inventory,new ItemStack(coin.item()),quantity)) return "space";
        wallets.set(player,balance-amount);commit(inventory,after);return "withdrawn";
    }
    public static String deposit(WalletStore wallets,Inventory inventory,UUID player,int denomination,int quantity,boolean all) throws IOException {
        var coin=CoinItems.entry(denomination);if(!all&&(coin==null||quantity<1||quantity>MAX_QUANTITY)) return "invalid";
        var after=copy(inventory);long amount=0;int left=quantity;
        for(int i=0;i<36;i++){var stack=after.get(i);if(!CoinItems.plain(stack)||(!all&&!stack.is(coin.item()))) continue;
            int take=all?stack.getCount():Math.min(left,stack.getCount());amount=Math.addExact(amount,Math.multiplyExact(CoinItems.faceValue(stack.getItem()),(long)take));stack.shrink(take);if(!all) left-=take;
        }
        if(amount==0||(!all&&left>0)) return "items";
        long balance=wallets.balance(player);if(balance>Money.MAX-amount) return "limit";
        wallets.set(player,balance+amount);commit(inventory,after);return "deposited";
    }
    public static String buyCreate(Catalogue catalogue,Inventory inventory,String id,int quantity){
        if(quantity<1||quantity>64) return "invalid";var entry=catalogue.entry(id);
        if(entry==null||!entry.buyable()||!id.startsWith("create:")) return "restricted";
        long cost=Money.total(entry.buy(),quantity),available=CoinItems.total(inventory);if(available<cost) return "funds";
        var after=copy(inventory);for(int i=0;i<36;i++) if(CoinItems.plain(after.get(i))) after.set(i,ItemStack.EMPTY);
        if(!add(after,inventory,new ItemStack(catalogue.item(id)),quantity)) return "space";
        long change=available-cost;
        for(int i=CoinItems.ALL.size()-1;i>=0;i--){var coin=CoinItems.ALL.get(i);long count=change/coin.value();change%=coin.value();
            if(count>Integer.MAX_VALUE||!add(after,inventory,new ItemStack(coin.item()),(int)count)) return "space";
        }
        if(change!=0) throw new IllegalStateException("Coin change mismatch");
        commit(inventory,after);return "bought";
    }
}
