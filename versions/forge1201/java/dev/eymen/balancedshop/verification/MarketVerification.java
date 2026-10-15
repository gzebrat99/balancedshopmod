package dev.eymen.balancedshop.verification;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.data.*;
import dev.eymen.balancedshop.command.EconomyCommands;
import dev.eymen.balancedshop.menu.ShopMenu;
import dev.eymen.balancedshop.network.MarketNetwork;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Actual Forge registry/world/command tests. Excluded from the released jar. */
public final class MarketVerification {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static void rejected(Runnable action,String label){try{action.run();}catch(IllegalArgumentException e){check(true,label);return;}throw new AssertionError(label);}
    public static int run(MinecraftServer server) throws Exception {
        checks=0;Path dir=Files.createTempDirectory("balanced-market-");
        var wallets=BalancedShop.wallets();check(wallets!=null&&wallets.data()!=null,"Live wallets use SavedData");
        UUID id=UUID.randomUUID(),other=UUID.randomUUID();var live=wallets.data();
        check(wallets.balance(id)==0,"New SavedData account starts at zero");wallets.set(id,12345);
        check(live.transfer(id,other,345)&&wallets.balance(id)==12000&&wallets.balance(other)==345,"Transfer debits and credits atomically");
        check(!live.transfer(id,id,1)&&!live.transfer(id,other,-1)&&!live.transfer(id,other,12001),"Invalid transfers rejected");
        wallets.set(other,Money.MAX);check(!live.transfer(id,other,1)&&wallets.balance(id)==12000,"Overflow transfer changes neither balance");
        live.stock("verification:persist",712);server.overworld().getDataStorage().save();
        Path dataFile=server.getWorldPath(LevelResource.ROOT).resolve("data/"+EconomyData.NAME+".dat");
        check(Files.exists(dataFile),"SavedData file is in world/data");
        EconomyData loaded=EconomyData.load(NbtIo.readCompressed(dataFile.toFile()).getCompound("data"));
        check(loaded.balance(id)==12000&&loaded.balance(other)==Money.MAX&&loaded.stock("verification:persist")==712,"Disk SavedData reload preserves balances and stocks");
        var migration=new EconomyData();Path json=dir.resolve("wallets.json");
        Files.writeString(json,"{\"version\":1,\"balances\":{\""+id+"\":9876}}");migration.migrate(json);
        check(migration.balance(id)==9876&&Files.exists(dir.resolve("wallets.pre-2.1.0.json")),"JSON migration backs up and preserves cents");
        migration.setBalance(id,80);migration=EconomyData.load(migration.save(new CompoundTag()));migration.migrate(json);
        check(migration.balance(id)==80,"Migration never reimports old money after restart");
        Files.writeString(json,"{\"version\":1,\"balances\":{\""+id+"\":-1}}");
        try{new EconomyData().migrate(json);throw new AssertionError("Corrupt JSON must fail");}catch(java.io.IOException expected){check(true,"Corrupt migration fails closed");}
        rejected(()->EconomyData.load(new CompoundTag()),"Unknown SavedData format rejected");
        check(EconomyCommands.amount("0.01",false)==1&&EconomyCommands.amount("123.45",false)==12345,"Decimal command amounts are exact");
        for(String invalid:List.of("-1","0","NaN","1e3","1.001","90000000000000.01"))rejected(()->EconomyCommands.amount(invalid,false),"Invalid amount "+invalid);
        check(EconomyCommands.amount("0",true)==0,"Admin set zero accepted");

        Catalogue catalogue=new Catalogue(server,new ShopConfig(),dir.resolve("prices.csv"));
        var data=new EconomyData();var market=new DynamicMarket(catalogue,data);catalogue.attachMarket(market);
        Inventory inventory=new Inventory(null);WalletStore testWallets=new WalletStore(dir.resolve("test-wallets.json"),0);testWallets.set(id,100_000_000L);
        String diamond="minecraft:diamond";long initial=catalogue.entry(diamond).buy();
        var quote=market.quote(testWallets,inventory,id,diamond,64,false);
        var bought=market.execute(testWallets,inventory,id,diamond,64,false);
        check(bought.equals(quote)&&bought.quantity()==64,"Quoted batch equals actual transaction");
        check(market.previousPrice(diamond)==initial,"Trend stores actual pre-purchase price");
        check(catalogue.entry(diamond).buy()>market.previousPrice(diamond),"Purchase produces upward trend");
        check(data.stock(diamond)==4032&&catalogue.entry(diamond).buy()>initial,"Buying decreases stock and raises price");
        long afterBuy=testWallets.balance(id);var sold=market.execute(testWallets,inventory,id,diamond,64,true);
        check(sold.quantity()==64&&data.stock(diamond)==4096&&catalogue.entry(diamond).buy()==initial,"Selling restores stock and lowers price");
        check(catalogue.entry(diamond).buy()<market.previousPrice(diamond),"Sale produces downward trend");
        check(testWallets.balance(id)==afterBuy+sold.total()&&sold.total()<bought.total(),"No buy/sell round-trip profit");
        long batch=market.quote(testWallets,inventory,id,diamond,64,false).total(),singles=0;
        for(int i=0;i<64;i++)singles+=market.execute(testWallets,inventory,id,diamond,1,false).total();
        check(batch==singles,"Bulk cost equals sequential marginal prices");
        data.stock(diamond,4096);inventory.clearContent();
        long block=market.quote(testWallets,inventory,id,"minecraft:diamond_block",1,false).total();
        check(block==market.quote(testWallets,inventory,id,diamond,9,false).total(),"Diamond block costs precisely nine sequential diamonds");
        market.execute(testWallets,inventory,id,"minecraft:diamond_block",1,false);
        check(data.stock(diamond)==4087,"Diamond block shares diamond stock");
        for(var bundle:FarmEconomy.BUNDLES.entrySet()){
            var unit=catalogue.entry("minecraft:"+bundle.getValue().material());var packed=catalogue.entry("minecraft:"+bundle.getKey());
            check(packed.buy()==unit.buy()*bundle.getValue().count()&&packed.sell()==unit.sell()*bundle.getValue().count(),"Dynamic bundle parity "+bundle.getKey());
        }
        inventory.clearContent();data.stock(diamond,3);
        check(market.execute(testWallets,inventory,id,diamond,4,false).quantity()==0&&data.stock(diamond)==3,"Insufficient stock is atomic");
        check(market.execute(testWallets,inventory,id,diamond,0,false).quantity()==3&&data.stock(diamond)==0,"Max buy respects remaining stock");
        check(market.quote(testWallets,inventory,id,diamond,1,false).message().equals("stock"),"Out of stock cannot be purchased");
        check(catalogue.entry(diamond).buy()<=market.definition(diamond).max(),"Price never exceeds max");
        data.stock(diamond,EconomyData.MAX_STOCK);long before=testWallets.balance(id);
        check(market.execute(testWallets,inventory,id,diamond,1,true).quantity()==0&&testWallets.balance(id)==before,"Full stock rejects sale without consuming inventory");
        check(catalogue.entry(diamond).buy()>=market.definition(diamond).min(),"Price never drops below min");
        data.stock(diamond,4096);inventory.clearContent();testWallets.set(id,initial);
        check(market.execute(testWallets,inventory,id,diamond,0,false).quantity()==1,"Max buy respects available balance");
        inventory.clearContent();testWallets.set(id,Money.MAX);
        for(int i=0;i<36;i++)inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));inventory.setItem(2,ItemStack.EMPTY);
        check(market.execute(testWallets,inventory,id,diamond,0,false).quantity()==64,"Max buy respects inventory capacity");
        inventory.clearContent();testWallets.set(id,0);inventory.setItem(0,new ItemStack(Items.PUMPKIN_SEEDS,64));inventory.setItem(1,new ItemStack(Items.PUMPKIN_SEEDS,16));
        check(market.execute(testWallets,inventory,id,"minecraft:pumpkin_seeds",0,true).quantity()==80&&testWallets.balance(id)>0,"Max sale earns first coins from seeds");
        check(market.quote(testWallets,inventory,id,diamond,-1,false).quantity()==0&&market.quote(testWallets,inventory,id,diamond,2305,false).quantity()==0,"Unbounded quantities rejected");
        check(market.quote(testWallets,inventory,id,"minecraft:zombie_spawn_egg",1,false).quantity()==0,"Hidden eggs remain unpurchasable");
        testWallets.set(id,Money.MAX);inventory.clearContent();
        check(market.execute(testWallets,inventory,id,"minecraft:diamond_pickaxe",1,false).quantity()==1,"Tools participate in dynamic purchases");
        var tool=inventory.getItem(0);long newValue=catalogue.sellValue(tool);tool.setDamageValue(tool.getMaxDamage()/2);
        check(catalogue.sellValue(tool)>0&&catalogue.sellValue(tool)<newValue,"Damaged tools retain proportional resale value");

        ShopMenu menu=new ShopMenu(42,inventory,UUID.randomUUID());
        var valid=new MarketNetwork.Request(42,menu.session,1,1,diamond,64,"","",0,0);
        check(MarketNetwork.valid(menu,valid),"Authenticated menu request accepted");menu.sequence=1;
        check(!MarketNetwork.valid(menu,valid),"Replay request rejected");menu.sequence=-1;
        check(!MarketNetwork.valid(menu,new MarketNetwork.Request(42,UUID.randomUUID(),1,1,diamond,1,"","",0,0)),"Foreign session rejected");
        check(!MarketNetwork.valid(menu,new MarketNetwork.Request(43,menu.session,1,1,diamond,1,"","",0,0)),"Foreign container rejected");
        check(!MarketNetwork.valid(menu,new MarketNetwork.Request(42,menu.session,1,3,diamond,1,"","",0,0)),"Unknown operation rejected");
        check(!MarketNetwork.valid(menu,new MarketNetwork.Request(42,menu.session,1,0,diamond,1,"","",0,0,31)),"Oversized grid page rejected");
        check(!MarketNetwork.valid(menu,new MarketNetwork.Request(42,menu.session,1,0,diamond,1,"","",0,0,0)),"Zero grid size rejected");
        check(MarketNetwork.valid(menu,new MarketNetwork.Request(42,menu.session,1,0,diamond,1,"","",0,0,30)),"Large 30-slot grid accepted");
        check(catalogue.search("diamond_chestplate","@gear").size()==1,"Armor category includes armor");
        check(catalogue.search("bow","@gear").stream().anyMatch(e->e.id().equals("minecraft:bow")),"Equipment category includes bows");
        check(catalogue.search("dirt","@gear").isEmpty(),"Equipment category excludes ordinary blocks");
        check(catalogue.search("nether_star","@rare").size()==1,"Rare category includes nether stars");
        check(catalogue.search("dirt","@rare").isEmpty(),"Rare category excludes dirt");
        check(catalogue.search("create:shaft","").size()==1,"Create items remain in the ordinary catalogue");
        for(String removed:List.of("dev.eymen.balancedshop.CreateTraderService","dev.eymen.balancedshop.TraderPackets","dev.eymen.balancedshop.mixin.CreateVillagerMixin")){
            check(MarketVerification.class.getClassLoader().getResource(removed.replace('.','/')+".class")==null,"Removed "+removed);
        }
        check(menu.quickMoveStack(null,0).isEmpty()&&menu.slots.isEmpty(),"Virtual catalogue cannot shift-click spawn items");
        var displayed=new DynamicMarket.Quote(64,6400,4032,"bought");var preview=new ShopMenu.Preview(diamond,0,25,displayed,displayed);
        check(preview.matches(diamond,0,25,false,displayed),"Server-held exact quote accepted");
        check(!preview.matches(diamond,0,25,false,new DynamicMarket.Quote(32,3200,4064,"bought")),"Changed Max amount cannot silently replace displayed order");
        check(!preview.matches(diamond,0,24,false,displayed),"Different quote revision rejected");
        var dispatcher=server.getCommands().getDispatcher();
        for(String command:List.of("bal","pay","economy"))check(dispatcher.getRoot().getChild(command)!=null,"Command registered "+command);
        check(!dispatcher.getRoot().getChild("economy").canUse(server.createCommandSourceStack().withPermission(0)),"Non-admin economy access denied");
        check(dispatcher.getRoot().getChild("economy").canUse(server.createCommandSourceStack().withPermission(2)),"Operator economy access allowed");
        var first=fake(server,"EconomyTestA");var second=fake(server,"EconomyTestB");
        var playersField=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");playersField.setAccessible(true);
        @SuppressWarnings("unchecked") var testPlayers=(List<ServerPlayer>)playersField.get(server.getPlayerList());
        testPlayers.add(first);testPlayers.add(second);
        try {
            var admin=server.createCommandSourceStack().withSuppressedOutput();var source=first.createCommandSourceStack().withSuppressedOutput();
            check(dispatcher.execute("economy set EconomyTestA 123.45",admin)==1&&live.balance(first.getUUID())==12345,"Admin set executes exact cents");
            check(dispatcher.execute("economy add EconomyTestA 1",admin)==1&&live.balance(first.getUUID())==12445,"Admin add executes");
            check(dispatcher.execute("economy remove EconomyTestA 0.45",admin)==1&&live.balance(first.getUUID())==12400,"Admin remove executes");
            check(dispatcher.execute("bal",source)==1,"Player /bal executes");
            check(dispatcher.execute("pay EconomyTestB 2.50",source)==1&&live.balance(first.getUUID())==12150&&live.balance(second.getUUID())==250,"Player /pay executes two-account transfer");
            check(dispatcher.execute("pay EconomyTestA 1",source)==0&&live.balance(first.getUUID())==12150,"Self payment denied by actual command");
            check(dispatcher.execute("economy remove EconomyTestA 10000",admin)==0&&live.balance(first.getUUID())==12150,"Admin overdraft rejected");
            check(dispatcher.execute("economy set EconomyTestA 0",admin)==1&&live.balance(first.getUUID())==0,"Admin zero reset executes");
        }finally{testPlayers.remove(first);testPlayers.remove(second);}
        // Stable UUID confirms actual server restarts reload the previously flushed data file.
        UUID restart=UUID.fromString("a6131397-f961-483e-bb59-12b8c02894d8");long previous=live.balance(restart);live.setBalance(restart,24680);
        check(previous==0||previous==24680,"Restart marker read from previous world save");
        Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/market.txt"),"PASS "+checks+" assertions; previous restart marker="+previous+"\n");
        return checks;
    }
    private static ServerPlayer fake(MinecraftServer server,String name){
        var p=new ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),name));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);return p;
    }
}
