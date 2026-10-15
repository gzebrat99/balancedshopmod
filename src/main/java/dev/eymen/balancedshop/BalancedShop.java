package dev.eymen.balancedshop;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class BalancedShop implements ModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("balancedshop");
    private static State state;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private record State(ShopConfig config, Catalogue catalogue, WalletStore wallets, Path directory) {}
    private static final class Session {
        ShopPackets.Query query=new ShopPackets.Query(0,0,"","");
        int lastQuery=Integer.MIN_VALUE/2, lastTrade=Integer.MIN_VALUE/2;
        boolean pendingQuery;
    }
    @Override public void onInitialize() {
        ModContent.initialize();
        CoinService.register(); CreateTraderService.register();
        SpawnerService.register(); VeinMiningService.register();
        SellService.register();
        BookService.register(); UpgradeService.register(); PriceSync.register();
        PayloadTypeRegistry.playC2S().register(ShopPackets.Query.TYPE,ShopPackets.Query.CODEC);
        PayloadTypeRegistry.playC2S().register(ShopPackets.Trade.TYPE,ShopPackets.Trade.CODEC);
        PayloadTypeRegistry.playS2C().register(ShopPackets.Page.TYPE,ShopPackets.Page.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            try {
                ShopConfig config=ShopConfig.load();
                Path directory=server.getWorldPath(LevelResource.ROOT).resolve("balancedshop");
                Catalogue catalogue=new Catalogue(server,config,directory.resolve("prices.csv"));
                WalletStore wallets=new WalletStore(directory.resolve("wallets.json"),config.startingBalance);

                BookCatalogue books=new BookCatalogue(server,config,directory.resolve("enchantments.csv"));
                catalogue.attachBooks(books);
        SpawnerCatalogue spawners=new SpawnerCatalogue(server,config);catalogue.attachSpawners(spawners);spawners.export(directory.resolve("spawners.csv"));
                state=new State(config,catalogue,wallets,directory);
                CoinService.configure(wallets,config); CreateTraderService.configure(catalogue,config);
                SpawnerService.configure(spawners,wallets,config); VeinMiningService.configure(config);
                SellService.configure(catalogue,wallets,config);
                BookService.configure(books,wallets,config); UpgradeService.configure(catalogue,books,wallets,config); PriceSync.configure(catalogue,server);
                LOG.info("Balanced Shop is ready.");
            } catch(Exception e) {
                state=null;CoinService.clear();CreateTraderService.clear();SpawnerService.clear();VeinMiningService.clear();SellService.clear();BookService.clear();UpgradeService.clear();PriceSync.clear();
                LOG.error("Shop disabled to preserve configuration/wallet data",e);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{state=null;sessions.clear();CoinService.clear();CreateTraderService.clear();SpawnerService.clear();VeinMiningService.clear();SellService.clear();BookService.clear();UpgradeService.clear();PriceSync.clear();});
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,resources,success)->{
            if(success && state!=null) try { reload(server); }
            catch(Exception e) { LOG.error("Shop reload failed; previous catalogue remains active",e); }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->sessions.remove(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var entry:sessions.entrySet()) {
                Session session=entry.getValue();
                if(session.pendingQuery && server.getTickCount()-session.lastQuery>=2) {
                    session.pendingQuery=false;session.lastQuery=server.getTickCount();
                    ServerPlayer player=server.getPlayerList().getPlayer(entry.getKey());
                    if(player!=null) respond(player,session,"");
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ShopPackets.Query.TYPE,(query,context)->{
            ServerPlayer player=context.player();
            Session session=sessions.computeIfAbsent(player.getUUID(),id->new Session());
            int now=player.level().getServer().getTickCount();
            session.query=new ShopPackets.Query(query.serial(),Math.max(0,query.page()),query.search().strip(),query.namespace());
            if(now-session.lastQuery<2) { session.pendingQuery=true;return; }
            session.lastQuery=now;session.pendingQuery=false;
            respond(player,session,"");
        });
        ServerPlayNetworking.registerGlobalReceiver(ShopPackets.Trade.TYPE,(trade,context)->{
            ServerPlayer player=context.player();
            Session session=sessions.computeIfAbsent(player.getUUID(),id->new Session());
            if(state==null) { respond(player,session,"unavailable"); return; }
            int now=player.level().getServer().getTickCount();
            if(now-session.lastTrade<state.config.transactionCooldownTicks) { respond(player,session,"cooldown");return; }
            session.lastTrade=now;
            String result;
            try {
                if(!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu) result="invalid";
                else {
                    result=TradeEngine.trade(state.catalogue,state.wallets,player.getInventory(),player.getUUID(),trade.item(),trade.quantity(),trade.sell());
                    if(result.equals("bought")||result.equals("sold")) {
                        player.inventoryMenu.broadcastChanges();
                        player.level().getServer().getPlayerList().save(player);
                        LOG.info("Trade player={} action={} item={} quantity={}",player.getUUID(),trade.sell()?"sell":"buy",trade.item(),trade.quantity());
                    }
                }
            } catch(Exception e) { LOG.error("Trade failed for {}",player.getUUID(),e); result="io"; }
            respond(player,session,result);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            dispatcher.register(Commands.literal("shop").executes(c->{
                ServerPlayer player=c.getSource().getPlayerOrException();
                respond(player,sessions.computeIfAbsent(player.getUUID(),id->new Session()),"");return 1;
            }));
            dispatcher.register(Commands.literal("balance").executes(c->{
                if(state==null) { c.getSource().sendFailure(Component.translatable("shop.balancedshop.unavailable"));return 0; }
                try {
                    long value=state.wallets.balance(c.getSource().getPlayerOrException().getUUID());
                    c.getSource().sendSuccess(()->Component.translatable("shop.balancedshop.balance",Money.format(value)),false);return 1;
                } catch(IOException e) { c.getSource().sendFailure(Component.translatable("shop.balancedshop.io"));return 0; }
            }));
            dispatcher.register(Commands.literal("shopadmin").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reload").executes(c->{
                    try { reload(c.getSource().getServer()); c.getSource().sendSuccess(()->Component.literal("Balanced Shop: prices reloaded."),true);return 1; }
                    catch(Exception e) { LOG.error("Configuration reload failed",e); c.getSource().sendFailure(Component.literal("Reload failed. Check balancedshop.json and server log."));return 0; }
                }))
                .then(Commands.literal("set").then(Commands.argument("player",EntityArgument.player())
                    .then(Commands.argument("coins",LongArgumentType.longArg(0,Money.MAX/100)).executes(c->{
                        if(state==null) return 0;
                        try {
                            ServerPlayer player=EntityArgument.getPlayer(c,"player");
                            long coins=LongArgumentType.getLong(c,"coins"); state.wallets.set(player.getUUID(),coins*100);
                            c.getSource().sendSuccess(()->Component.literal("Balance: "+Money.format(coins*100)),true);
                            if(sessions.containsKey(player.getUUID())) respond(player,sessions.get(player.getUUID()),"");return 1;
                        } catch(IOException e) { c.getSource().sendFailure(Component.translatable("shop.balancedshop.io"));return 0; }
                    })))));
        });
    }
    private static void reload(MinecraftServer server) throws IOException {
        if(state==null) throw new IOException("Shop is unavailable; fix the data and restart the server");
        ShopConfig config=ShopConfig.load();
        Catalogue catalogue=new Catalogue(server,config,state.directory.resolve("prices.csv"));

        BookCatalogue books=new BookCatalogue(server,config,state.directory.resolve("enchantments.csv"));
        catalogue.attachBooks(books);
        SpawnerCatalogue spawners=new SpawnerCatalogue(server,config);catalogue.attachSpawners(spawners);spawners.export(state.directory.resolve("spawners.csv"));
        // Starting balance only applies to wallets first created after the next restart.
        state=new State(config,catalogue,state.wallets,state.directory);
        CoinService.configure(state.wallets,config); CreateTraderService.configure(catalogue,config);
        SpawnerService.configure(spawners,state.wallets,config); VeinMiningService.configure(config);
        SellService.configure(catalogue,state.wallets,config);
        BookService.configure(books,state.wallets,config); UpgradeService.configure(catalogue,books,state.wallets,config); PriceSync.configure(catalogue,server);
        for(ServerPlayer player:server.getPlayerList().getPlayers())
            if(sessions.containsKey(player.getUUID())) respond(player,sessions.get(player.getUUID()),"reloaded");
    }
    private static void respond(ServerPlayer player,Session session,String message) {
        if(!ServerPlayNetworking.canSend(player,ShopPackets.Page.TYPE)) {
            player.sendSystemMessage(Component.literal("Balanced Shop must be installed on the client and server.")); return;
        }
        if(state==null) {
            ServerPlayNetworking.send(player,new ShopPackets.Page(session.query.serial(),0,0,0,"unavailable",List.of(),List.of()));return;
        }
        try {
            List<ShopPackets.Entry> found=state.catalogue.search(session.query.search(),session.query.namespace());
            int pages=Math.max(1,(found.size()+Catalogue.PAGE_SIZE-1)/Catalogue.PAGE_SIZE);
            int page=Math.min(session.query.page(),pages-1), start=page*Catalogue.PAGE_SIZE;
            ServerPlayNetworking.send(player,new ShopPackets.Page(session.query.serial(),page,found.size(),state.wallets.balance(player.getUUID()),message,
                state.catalogue.namespaces(),List.copyOf(found.subList(start,Math.min(start+Catalogue.PAGE_SIZE,found.size())))));
        } catch(Exception e) {
            LOG.error("Could not send shop catalogue",e);
            ServerPlayNetworking.send(player,new ShopPackets.Page(session.query.serial(),0,0,0,"io",List.of(),List.of()));
        }
    }
}
