package dev.eymen.balancedshop.data;

import dev.eymen.balancedshop.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.util.*;

/** Deterministic marginal pricing: buying 64 at once costs exactly as much as 64 sequential purchases. */
public final class DynamicMarket {
    public static final int MAX_QUANTITY=2304;
    private final Catalogue catalogue;
    private final EconomyData data;
    private final Map<String,Long> previousStocks=new HashMap<>();
    public record Pool(String id,int units){}
    public record Definition(long base,long min,long max,long stock){}
    public record Quote(int quantity,long total,long afterStock,String message){}
    public DynamicMarket(Catalogue catalogue,EconomyData data){this.catalogue=catalogue;this.data=data;}
    public EconomyData data(){return data;}
    /** Last committed trade's previous price, shared by packed blocks and their materials. No fabricated history. */
    public long previousPrice(String id){
        Pool p=pool(id);Long before=previousStocks.get(p.id());
        if(before==null)return catalogue.entry(id).buy();
        return price(catalogue.baseEntry(p.id()).buy(),before,p.units())*p.units();
    }
    public Pool pool(String id){
        int units=1;
        // Packed mineral/farm blocks share the underlying material stock, including nested bundles.
        for(int i=0;i<4 && id.startsWith("minecraft:");i++){
            var bundle=FarmEconomy.BUNDLES.get(id.substring(10));if(bundle==null)break;
            units=Math.multiplyExact(units,bundle.count());id="minecraft:"+bundle.material();
        }
        return new Pool(id,units);
    }
    public Definition definition(String id){
        Pool p=pool(id);var base=catalogue.baseEntry(p.id());
        return new Definition(base.buy()*p.units(),Math.max(1,base.buy()/2)*p.units(),Math.min(Money.MAX_PRICE/p.units(),base.buy()*2)*p.units(),data.stock(p.id())/p.units());
    }
    private long price(long base,long stock,int units){
        long lower=Math.max(1,base/2),upper=Math.min(Money.MAX_PRICE/units,base*2);
        return Math.max(Math.min(lower,upper),Math.min(upper,base*(2*EconomyData.INITIAL_STOCK)/(EconomyData.INITIAL_STOCK+stock)));
    }
    private long sellUnit(ShopPackets.Entry base,long buy){
        // Integer money throughout; avoid overflow even for administrator-defined high prices.
        return base.sellable() ? Math.max(1,java.math.BigInteger.valueOf(buy).multiply(java.math.BigInteger.valueOf(base.sell())).divide(java.math.BigInteger.valueOf(base.buy())).min(java.math.BigInteger.valueOf(buy)).longValue()) : 0;
    }
    public ShopPackets.Entry current(ShopPackets.Entry base){
        if(base==null || CoinItems.faceValue(catalogue.item(base.id()))>0 || Catalogue.hidden(base.id()))return base;
        Pool p=pool(base.id());var unit=catalogue.baseEntry(p.id());long buy=price(unit.buy(),data.stock(p.id()),p.units());
        return new ShopPackets.Entry(base.id(),buy*p.units(),sellUnit(unit,buy)*p.units(),base.buyable(),base.sellable());
    }
    private boolean matches(ItemStack stack,String id){return !stack.isEmpty()&&TradeEngine.saleKey(stack).equals(id);}
    public Quote quote(WalletStore wallets,Inventory inventory,UUID player,String id,int requested,boolean sell) throws IOException {
        var entry=catalogue.baseEntry(id);
        if(entry==null||CoinItems.faceValue(catalogue.item(id))>0||(sell?!entry.sellable():!entry.buyable()))return new Quote(0,0,0,"restricted");
        if(requested<0||requested>MAX_QUANTITY)return new Quote(0,0,0,"invalid");
        Pool p=pool(id);var base=catalogue.baseEntry(p.id());long stock=data.stock(p.id()),balance=wallets.balance(player);
        ItemStack prototype=new ItemStack(catalogue.item(id));
        List<ItemStack> offered=new ArrayList<>();int space=0;
        for(int i=0;i<36;i++){
            var stack=inventory.getItem(i);
            if(sell){if(matches(stack,id)&&catalogue.sellValue(stack)>0)for(int j=0;j<stack.getCount();j++)offered.add(stack);}
            else if(stack.isEmpty())space+=Math.min(inventory.getMaxStackSize(),prototype.getMaxStackSize());
            else if(ItemStack.isSameItemSameTags(stack,prototype))space+=Math.max(0,Math.min(inventory.getMaxStackSize(),prototype.getMaxStackSize())-stack.getCount());
        }
        int limit=requested==0?MAX_QUANTITY:requested,count=0;long total=0;String failure=sell?"items":"space";
        for(;count<limit;count++){
            if(sell?count>=offered.size():count>=space){failure=sell?"items":"space";break;}
            if(sell?stock>EconomyData.MAX_STOCK-p.units():stock<p.units()){failure="stock";break;}
            long marginal=0,buyBasis=0;
            for(int j=0;j<p.units();j++){
                long at=sell?stock+j+1:stock-j; // A sale is paid at its destination stock: no round-trip profit.
                long unitBuy=price(base.buy(),at,p.units());buyBasis+=unitBuy;
                marginal+=sell?sellUnit(base,unitBuy):unitBuy;
            }
            if(sell){
                ItemStack offeredStack=offered.get(count);
                marginal=ItemValues.sell(offeredStack,buyBasis,marginal,true,(enchant,level)->catalogue.enchantmentPrices().getOrDefault(enchant+"@"+level,0L));
            }
            if(!sell&&marginal>balance-total){failure="funds";break;}
            if(sell&&marginal>Money.MAX-balance-total){failure="limit";break;}
            if(marginal<=0){failure="restricted";break;}
            total+=marginal;stock+=sell?p.units():-p.units();
        }
        if(count==0 || (requested!=0&&count!=requested))return new Quote(0,0,data.stock(p.id()),failure);
        return new Quote(count,total,stock,sell?"sold":"bought");
    }
    public Quote execute(WalletStore wallets,Inventory inventory,UUID player,String id,int requested,boolean sell) throws IOException {
        Quote q=quote(wallets,inventory,player,id,requested,sell);if(q.quantity()==0)return q;
        List<ItemStack> result=new ArrayList<>();for(int i=0;i<36;i++)result.add(inventory.getItem(i).copy());
        ItemStack prototype=new ItemStack(catalogue.item(id));int remaining=q.quantity();
        for(int pass=0;pass<(sell?1:2)&&remaining>0;pass++)for(int i=0;i<36&&remaining>0;i++){
            var stack=result.get(i);
            if(sell&&matches(stack,id)&&catalogue.sellValue(stack)>0){int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;}
            else if(!sell&&pass==0&&!stack.isEmpty()&&ItemStack.isSameItemSameTags(stack,prototype)){
                int n=Math.min(remaining,Math.max(0,Math.min(inventory.getMaxStackSize(),stack.getMaxStackSize())-stack.getCount()));stack.grow(n);remaining-=n;
            }else if(!sell&&pass==1&&stack.isEmpty()){
                int n=Math.min(remaining,Math.min(inventory.getMaxStackSize(),prototype.getMaxStackSize()));result.set(i,prototype.copyWithCount(n));remaining-=n;
            }
        }
        if(remaining!=0)throw new IllegalStateException("Trade plan invariant");
        // Commit only after every check/plan succeeds, in a single server-thread task.
        long balance=wallets.balance(player);wallets.set(player,sell?balance+q.total():balance-q.total());
        String poolId=pool(id).id();long previousStock=data.stock(poolId);
        data.stock(poolId,q.afterStock());previousStocks.put(poolId,previousStock);
        for(int i=0;i<36;i++)inventory.setItem(i,result.get(i));inventory.setChanged();
        return q;
    }
}
