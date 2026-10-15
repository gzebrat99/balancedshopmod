package dev.eymen.balancedshop;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class SellService {
    private static Catalogue catalogue;
    private static WalletStore wallets;
    private static int cooldown;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Session {int serial,lastQuery=Integer.MIN_VALUE/2,lastAction=Integer.MIN_VALUE/2;boolean pending;}
    private SellService() {}
    public static void configure(Catalogue prices,WalletStore store,ShopConfig config){catalogue=prices;wallets=store;cooldown=config.transactionCooldownTicks;sessions.values().forEach(s->s.pending=true);}
    public static void clear(){catalogue=null;wallets=null;sessions.clear();}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(SellPackets.Query.TYPE,SellPackets.Query.CODEC);
        PayloadTypeRegistry.playC2S().register(SellPackets.Action.TYPE,SellPackets.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(SellPackets.Page.TYPE,SellPackets.Page.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->sessions.remove(handler.getPlayer().getUUID()));
        ServerPlayNetworking.registerGlobalReceiver(SellPackets.Query.TYPE,(query,context)->{
            Session s=sessions.computeIfAbsent(context.player().getUUID(),id->new Session());s.serial=query.serial();s.pending=true;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var entry:sessions.entrySet()){
                Session s=entry.getValue();if(!s.pending||server.getTickCount()-s.lastQuery<2) continue;
                s.pending=false;s.lastQuery=server.getTickCount();var player=server.getPlayerList().getPlayer(entry.getKey());
                if(player!=null) reply(player,s,"");
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(SellPackets.Action.TYPE,(action,context)->{
            ServerPlayer player=context.player();Session s=sessions.computeIfAbsent(player.getUUID(),id->new Session());
            int now=player.level().getServer().getTickCount();String result;
            if(now-s.lastAction<cooldown){reply(player,s,"cooldown");return;}
            s.lastAction=now;
            try {
                if(catalogue==null) result="unavailable";
                else if(action.serial()!=s.serial||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu) result="invalid";
                else {
                    result=TradeEngine.sell(catalogue,wallets,player.getInventory(),player.getUUID(),action.item(),action.quantity());
                    if(result.equals("sold")){
                        player.inventoryMenu.broadcastChanges();player.level().getServer().getPlayerList().save(player);
                        BalancedShop.LOG.info("Sell GUI player={} item={} quantity={}",player.getUUID(),action.item(),action.quantity()==0?"all":action.quantity());
                    }
                }
            }catch(Exception e){BalancedShop.LOG.error("Sell GUI transaction failed",e);result="io";}
            reply(player,s,result);
        });
    }
    private static void reply(ServerPlayer player,Session session,String message){
        if(!ServerPlayNetworking.canSend(player,SellPackets.Page.TYPE)) return;
        try {
            List<SellPackets.Slot> slots=new ArrayList<>(36);
            for(int i=0;i<36;i++){
                ItemStack stack=player.getInventory().getItem(i);String id=stack.isEmpty()?"":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                var entry=catalogue==null?null:catalogue.entry(id);
                long value=catalogue==null?0:catalogue.sellValue(stack);
                boolean plain=value>0;
                slots.add(new SellPackets.Slot(stack.isEmpty()?"":TradeEngine.saleKey(stack),stack.getCount(),plain,entry!=null&&entry.sellable(),value,stack.copy()));
            }
            ServerPlayNetworking.send(player,new SellPackets.Page(session.serial,wallets==null?0:wallets.balance(player.getUUID()),catalogue==null?"unavailable":message,List.copyOf(slots)));
        }catch(Exception e){
            BalancedShop.LOG.error("Sell GUI preview failed",e);
            ServerPlayNetworking.send(player,new SellPackets.Page(session.serial,0,"io",Collections.nCopies(36,new SellPackets.Slot("",0,false,false,0))));
        }
    }
}
