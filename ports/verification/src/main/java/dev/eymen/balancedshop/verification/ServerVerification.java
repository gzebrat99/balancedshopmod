package dev.eymen.balancedshop.verification;
import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.bridge.ServerBridge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import java.nio.file.*;
import java.util.*;
public final class ServerVerification {
    public static volatile boolean ready;public static int assertions;
    public static void check(boolean b,String message){if(!b)throw new AssertionError(message);assertions++;}
    public static void registerItems(){for(String path:List.of("shaft","andesite_alloy")){var id=Identifier.parse("create:"+path);Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));}}
    public static void install(){ServerBridge.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
        Files.createDirectories(Path.of("verification"));var cfg=new ShopConfig();var c=new Catalogue(server,cfg,Path.of("verification/prices.csv"));
        var books=new BookCatalogue(server,cfg,Path.of("verification/books.csv"));c.attachBooks(books);
        var wallet=new WalletStore(Files.createTempDirectory("balancedshop-native-").resolve("wallets.json"),0);var id=UUID.randomUUID();var inventory=new Inventory(null,new EntityEquipment());
        check(wallet.balance(id)==0,"Zero initial balance");check(c.all().size()==BuiltInRegistries.ITEM.size()-1,"Every registered item priced");
        check(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse("balancedshop:uncrafting_table"))).isPresent(),"Native uncrafting table recipe loaded");
        check(books.all().size()>100,"All enchantment levels available");check(books.entry("minecraft:mending",1)!=null&&books.entry("minecraft:unbreaking",3)!=null,"Mending and Unbreaking III");
        inventory.setItem(0,new ItemStack(Items.DIAMOND,64));long sale=c.sellValue(inventory.getItem(0));
        check(TradeEngine.trade(c,wallet,inventory,id,"minecraft:diamond",16,true).equals("sold"),"Native sale");check(wallet.balance(id)==sale*16,"Exact sale amount");
        check(TradeEngine.trade(c,wallet,inventory,id,"minecraft:diamond",1,false).equals("bought"),"Native purchase");check(inventory.getItem(0).getCount()==49,"Correct item amount");
        inventory.clearContent();wallet.set(id,100000000);
        for(int denomination=0;denomination<4;denomination++)check(CoinEngine.withdraw(wallet,inventory,id,denomination,16).equals("withdrawn"),"Each coin denomination withdraws");
        check(wallet.balance(id)+CoinItems.total(inventory)==100000000,"Withdrawal conserves value");
        check(CoinEngine.deposit(wallet,inventory,id,0,0,true).equals("deposited"),"Deposit all");check(wallet.balance(id)==100000000,"Deposit conservation");
        check(CoinEngine.buyCreate(c,inventory,"create:shaft",1).equals("funds"),"Bank balance cannot pay villagers");
        CoinEngine.withdraw(wallet,inventory,id,2,64);long cash=CoinItems.total(inventory);long before=wallet.balance(id);
        check(CoinEngine.buyCreate(c,inventory,"create:shaft",16).equals("bought"),"Native Create fixture cash payment");check(CoinItems.total(inventory)==cash-c.entry("create:shaft").buy()*16,"Exact change");check(wallet.balance(id)==before,"Villager preserves bank balance");
        check(CoinEngine.buyCreate(c,inventory,"minecraft:diamond",1).equals("restricted"),"Vanilla cannot be bought at trader");check(CreateTraderService.offers(c,"").stream().allMatch(e->e.id().startsWith("create:")),"Create-only catalogue");
        for(var coin:CoinItems.ALL){check(!c.entry(coin.id()).buyable()&&!c.entry(coin.id()).sellable(),"No currency minting through shop");}
        inventory.clearContent();check(BookPurchase.buy(books,wallet,inventory,id,"minecraft:mending",1,1).equals("bought"),"Book purchase with native registry");check(inventory.getItem(0).is(Items.ENCHANTED_BOOK),"Actual enchanted book");
        inventory.clearContent();inventory.setItem(0,new ItemStack(Items.OAK_PLANKS,4));var reverse=new UncraftingCatalogue(server,c);var plan=reverse.recipe("minecraft:oak_planks");check(plan!=null,"Native reverse recipe");
        check(UncraftingEngine.uncraft(reverse,c,wallet,inventory,id,0,plan.recipe(),plan.input(),1).equals("done"),"Uncraft engine");check(inventory.getItem(0).is(Items.OAK_LOG),"Returns whole log");
        inventory.clearContent();inventory.setItem(0,new ItemStack(Items.DIAMOND,16));var target=new UpgradeEngine.Target("minecraft:netherite_ingot",new ItemStack(Items.NETHERITE_INGOT),c.entry("minecraft:netherite_ingot").buy());
        var quote=UpgradeEngine.quote(inventory,0,target,c.sellValue(inventory.getItem(0)),16,1,true);check(quote.chance()<=.9,"Chance maximum 90 percent");
        check(UpgradeEngine.execute(quote,wallet,inventory,id,0,true,()->0).equals("success"),"Deterministic success");check(inventory.getItem(0).is(Items.NETHERITE_INGOT),"Correct reward");
        inventory.clearContent();inventory.setItem(0,new ItemStack(Items.DIAMOND,16));quote=UpgradeEngine.quote(inventory,0,target,c.sellValue(inventory.getItem(0)),16,1,true);
        check(UpgradeEngine.execute(quote,wallet,inventory,id,0,true,()->1).equals("failed"),"Deterministic failure");check(inventory.getItem(0).isEmpty(),"Entire selected stake lost");
        Files.writeString(Path.of("verification/server.txt"),"PASS: "+assertions+" native server assertions\n");ready=true;BalancedShop.LOG.info("PORT VERIFICATION SERVER PASS: {} assertions",assertions);
    }catch(Exception e){throw new AssertionError(e);}});}
}
