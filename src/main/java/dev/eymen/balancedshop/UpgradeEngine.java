package dev.eymen.balancedshop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import java.io.IOException;
import java.util.*;
import java.util.function.DoubleSupplier;

/** Server-owned quotes and atomic inventory/wallet updates. All amounts are cents. */
public final class UpgradeEngine {
    public static final int MAX_QUANTITY=2304;
    public record Target(String key,ItemStack result,long value) {}
    public record Quote(ItemStack source,Target target,long sourceValue,double chance,long fee,int give,int receive,List<ItemStack> snapshot) {}
    private UpgradeEngine() {}
    public static long value(Catalogue catalogue,BookCatalogue books,ItemStack stack){
        if(stack.isEmpty()) return 0;
        var entry=catalogue.entry(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(entry==null||!entry.buyable()) return 0;
        long value=entry.buy();
        for(var enchant:EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()){
            var book=books.entry(enchant.getKey().unwrapKey().orElseThrow().identifier().toString(),enchant.getIntValue());
            if(book==null) return 0;
            value+=book.buy();
        }
        if(stack.isDamageableItem()) value=Math.max(1,Math.round(value*(1.0-(double)stack.getDamageValue()/stack.getMaxDamage())));
        return value;
    }
    public static boolean eligible(ItemStack stack,boolean enchant){
        if(stack.isEmpty()) return false;
        if(!enchant) return ItemValues.sellableComponents(stack);
        ItemStack clean=stack.copyWithCount(1),base=new ItemStack(stack.getItem());
        if(enchant){
            clean.remove(DataComponents.DAMAGE);base.remove(DataComponents.DAMAGE);
            clean.remove(DataComponents.ENCHANTMENTS);base.remove(DataComponents.ENCHANTMENTS);
            clean.remove(DataComponents.STORED_ENCHANTMENTS);base.remove(DataComponents.STORED_ENCHANTMENTS);
            clean.remove(DataComponents.REPAIR_COST);base.remove(DataComponents.REPAIR_COST);
            clean.remove(DataComponents.CUSTOM_NAME);base.remove(DataComponents.CUSTOM_NAME);
        }
        return ItemStack.isSameItemSameComponents(clean,base);
    }
    public static long stakeValue(Catalogue catalogue,ItemStack source,boolean enchant){
        ItemStack valued=source.copy();if(enchant) valued.remove(DataComponents.CUSTOM_NAME);return catalogue.sellValue(valued);
    }
    public static List<Target> targets(Catalogue catalogue,BookCatalogue books,ItemStack source,boolean enchant,String search){
        if(!eligible(source,enchant)) return List.of();
        long sourceValue=value(catalogue,books,source);if(sourceValue<=0||stakeValue(catalogue,source,enchant)<=0) return List.of();
        String query=search.strip().toLowerCase(Locale.ROOT);List<Target> found=new ArrayList<>();
        if(!enchant){
            for(var entry:catalogue.all()) if(entry.buyable()&&catalogue.item(entry.id())!=source.getItem()){
                var result=new ItemStack(catalogue.item(entry.id()));
                if(query.isEmpty()||entry.id().contains(query)||result.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) found.add(new Target(entry.id(),result,entry.buy()));
            }
        }else{
            var existing=EnchantmentHelper.getEnchantmentsForCrafting(source);
            for(var book:books.all()){
                var holder=book.book().get(DataComponents.STORED_ENCHANTMENTS).keySet().iterator().next();
                if(source.getItem()!=Items.BOOK&&source.getItem()!=Items.ENCHANTED_BOOK&&!holder.value().canEnchant(source)) continue;
                if(existing.getLevel(holder)>=book.level()) continue;
                if(existing.keySet().stream().anyMatch(other->!other.equals(holder)&&!Enchantment.areCompatible(other,holder))) continue;
                if(!query.isEmpty()&&!book.id().contains(query)&&!BookCatalogue.name(book.book()).getString().toLowerCase(Locale.ROOT).contains(query)) continue;
                ItemStack result=source.copyWithCount(1);
                if(result.is(Items.BOOK)) {result=new ItemStack(Items.ENCHANTED_BOOK);var name=source.get(DataComponents.CUSTOM_NAME);if(name!=null) result.set(DataComponents.CUSTOM_NAME,name);}
                ItemEnchantments.Mutable mutable=new ItemEnchantments.Mutable(existing);mutable.set(holder,book.level());
                EnchantmentHelper.setEnchantments(result,mutable.toImmutable());
                long targetValue=value(catalogue,books,source)+book.buy();
                int old=existing.getLevel(holder);var oldBook=books.entry(book.id(),old);
                if(oldBook!=null) targetValue-=oldBook.buy();
                if(source.isDamageableItem()) targetValue=value(catalogue,books,result);
                if(targetValue>sourceValue) found.add(new Target(book.id()+"@"+book.level(),result,targetValue));
            }
        }
        found.sort(Comparator.comparingLong(Target::value).thenComparing(Target::key));return List.copyOf(found);
    }
    public static double chance(long source,long target){return Math.max(.001,Math.min(.90,.65*(double)source/target));}
    public static long fee(long source,long target,double chance,boolean destroy){
        double gain=destroy?chance*target-source:chance*(target-source);
        return Math.max(2500,(long)Math.ceil(Math.max(0,gain)*1.25+source*.02));
    }
    public static Quote quote(ItemStack source,Target target,long value,boolean destroy){
        double chance=chance(value,target.value());return new Quote(source.copy(),target,value,chance,fee(value,target.value(),chance,destroy),1,1,List.of());
    }
    public static int available(Inventory inventory,ItemStack source){int count=0;for(int i=0;i<36;i++) if(ItemStack.isSameItemSameComponents(inventory.getItem(i),source)) count+=inventory.getItem(i).getCount();return count;}
    public static Quote quote(Inventory inventory,int slot,Target target,long unitSell,int give,int receive,boolean destroy){
        if(slot<0||slot>=36||give<1||give>MAX_QUANTITY||receive<1||receive>MAX_QUANTITY||unitSell<=0) return null;
        ItemStack source=inventory.getItem(slot);if(source.isEmpty()||available(inventory,source)<give) return null;
        long value=Math.multiplyExact(unitSell,(long)give),goal=Math.multiplyExact(target.value(),(long)receive);double chance=chance(value,goal);
        List<ItemStack> snapshot=new ArrayList<>(36);for(int i=0;i<36;i++) snapshot.add(inventory.getItem(i).copy());
        return new Quote(source.copy(),target,value,chance,fee(value,goal,chance,destroy),give,receive,List.copyOf(snapshot));
    }
    public static String execute(Quote quote,WalletStore wallets,Inventory inventory,UUID player,int slot,boolean destroy,DoubleSupplier random) throws IOException {
        if(quote==null||slot<0||slot>=36) return "invalid";
        ItemStack current=inventory.getItem(slot);
        if(current.getCount()!=quote.source().getCount()||!ItemStack.isSameItemSameComponents(current,quote.source())) return "changed";
        if(!quote.snapshot().isEmpty()) for(int i=0;i<36;i++){
            var old=quote.snapshot().get(i);var now=inventory.getItem(i);
            if(ItemStack.isSameItemSameComponents(old,quote.source())||ItemStack.isSameItemSameComponents(now,quote.source()))
                if(old.getCount()!=now.getCount()||!ItemStack.isSameItemSameComponents(old,now)) return "changed";
        }
        if(available(inventory,current)<quote.give()) return "items";
        long balance=wallets.balance(player);if(balance<quote.fee()) return "funds";
        List<ItemStack> after=new ArrayList<>(36);for(int i=0;i<36;i++) after.add(inventory.getItem(i).copy());
        int remaining=quote.give();
        for(int order=-1;order<36&&remaining>0;order++){
            int i=order<0?slot:order;if(order==slot) continue;
            var stack=after.get(i);if(ItemStack.isSameItemSameComponents(stack,current)){int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;}
        }
        List<ItemStack> consumed=after.stream().map(ItemStack::copy).toList();
        // Verify the entire reward fits before rolling or charging; never drop excess rewards.
        ItemStack result=quote.target().result();int left=quote.receive(),maximum=Math.min(inventory.getMaxStackSize(),result.getMaxStackSize());
        for(int pass=0;pass<2&&left>0;pass++) for(int i=0;i<36&&left>0;i++){
            var stack=after.get(i);
            if(pass==0&&!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,result)){int put=Math.min(left,Math.max(0,maximum-stack.getCount()));stack.grow(put);left-=put;}
            else if(pass==1&&stack.isEmpty()){int put=Math.min(left,maximum);after.set(i,result.copyWithCount(put));left-=put;}
        }
        if(left>0) return "space";
        boolean success=random.getAsDouble()<quote.chance();
        if(!success){after=new ArrayList<>(36);for(int i=0;i<36;i++) after.add(destroy?consumed.get(i):inventory.getItem(i).copy());}
        wallets.set(player,balance-quote.fee());
        for(int i=0;i<36;i++) inventory.setItem(i,after.get(i));inventory.setChanged();
        return success?"success":"failed";
    }
}
