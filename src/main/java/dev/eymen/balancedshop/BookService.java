package dev.eymen.balancedshop;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class BookService {
    private static BookCatalogue catalogue;
    private static WalletStore wallets;
    private static int cooldown;
    private static long generation;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Session {BookPackets.Query query=new BookPackets.Query(0,0,"");int lastQuery=Integer.MIN_VALUE/2,lastBuy=Integer.MIN_VALUE/2;boolean pending;}
    private BookService() {}
    public static void configure(BookCatalogue books,WalletStore store,ShopConfig config){catalogue=books;wallets=store;cooldown=config.transactionCooldownTicks;generation++;sessions.values().forEach(s->s.pending=true);}
    public static void clear(){catalogue=null;wallets=null;sessions.clear();generation++;}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(BookPackets.Query.TYPE,BookPackets.Query.CODEC);
        PayloadTypeRegistry.playC2S().register(BookPackets.Buy.TYPE,BookPackets.Buy.CODEC);
        PayloadTypeRegistry.playS2C().register(BookPackets.Page.TYPE,BookPackets.Page.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->sessions.remove(handler.getPlayer().getUUID()));
        ServerPlayNetworking.registerGlobalReceiver(BookPackets.Query.TYPE,(query,context)->{
            var session=sessions.computeIfAbsent(context.player().getUUID(),id->new Session());session.query=query;session.pending=true;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var entry:sessions.entrySet()){
                var session=entry.getValue();if(!session.pending||server.getTickCount()-session.lastQuery<2) continue;
                session.pending=false;session.lastQuery=server.getTickCount();var player=server.getPlayerList().getPlayer(entry.getKey());if(player!=null) reply(player,session,"");
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(BookPackets.Buy.TYPE,(buy,context)->{
            var player=context.player();var session=sessions.computeIfAbsent(player.getUUID(),id->new Session());int now=player.level().getServer().getTickCount();String result;
            if(now-session.lastBuy<cooldown){reply(player,session,"cooldown");return;}session.lastBuy=now;
            try {
                if(catalogue==null) result="unavailable";
                else if(buy.generation()!=generation) result="changed";
                else if(buy.serial()!=session.query.serial()||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu) result="invalid";
                else {
                    result=BookPurchase.buy(catalogue,wallets,player.getInventory(),player.getUUID(),buy.id(),buy.level(),buy.quantity());
                    if(result.equals("bought")){
                        player.inventoryMenu.broadcastChanges();player.level().getServer().getPlayerList().save(player);
                        BalancedShop.LOG.info("Book purchase player={} enchantment={} level={} quantity={}",player.getUUID(),buy.id(),buy.level(),buy.quantity());
                    }
                }
            }catch(Exception e){BalancedShop.LOG.error("Enchanted book purchase failed",e);result="io";}
            reply(player,session,result);
        });
    }
    private static void reply(ServerPlayer player,Session session,String message){
        if(!ServerPlayNetworking.canSend(player,BookPackets.Page.TYPE)) return;
        try {
            var found=catalogue==null?List.<BookCatalogue.Entry>of():catalogue.search(session.query.search());
            int page=Math.max(0,Math.min(session.query.page(),Math.max(0,(found.size()-1)/BookCatalogue.PAGE_SIZE))),start=page*BookCatalogue.PAGE_SIZE;
            ServerPlayNetworking.send(player,new BookPackets.Page(session.query.serial(),generation,page,found.size(),wallets==null?0:wallets.balance(player.getUUID()),catalogue==null?"unavailable":message,
                List.copyOf(found.subList(start,Math.min(start+BookCatalogue.PAGE_SIZE,found.size())))));
        }catch(Exception e){BalancedShop.LOG.error("Book catalogue preview failed",e);ServerPlayNetworking.send(player,new BookPackets.Page(session.query.serial(),generation,0,0,0,"io",List.of()));}
    }
}
