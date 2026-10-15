package dev.eymen.balancedshop;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class CoinService {
    private static WalletStore wallets;private static int cooldown;private static long token;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Session {int serial,lastQuery=Integer.MIN_VALUE/2,lastAction=Integer.MIN_VALUE/2;long token;boolean pending;}
    public static void configure(WalletStore w,ShopConfig cfg){wallets=w;cooldown=cfg.transactionCooldownTicks;sessions.values().forEach(s->{s.token=++token;s.pending=true;});}
    public static void clear(){wallets=null;sessions.clear();}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(CoinPackets.Query.TYPE,CoinPackets.Query.CODEC);PayloadTypeRegistry.playC2S().register(CoinPackets.Action.TYPE,CoinPackets.Action.CODEC);PayloadTypeRegistry.playS2C().register(CoinPackets.Page.TYPE,CoinPackets.Page.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->sessions.remove(handler.getPlayer().getUUID()));
        ServerPlayNetworking.registerGlobalReceiver(CoinPackets.Query.TYPE,(q,context)->{var s=sessions.computeIfAbsent(context.player().getUUID(),id->new Session());s.serial=q.serial();s.token=++token;s.pending=true;});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var entry:sessions.entrySet()){var s=entry.getValue();if(s.pending&&server.getTickCount()-s.lastQuery>=2){s.pending=false;s.lastQuery=server.getTickCount();var player=server.getPlayerList().getPlayer(entry.getKey());if(player!=null) reply(player,s,"");}}});
        ServerPlayNetworking.registerGlobalReceiver(CoinPackets.Action.TYPE,(a,context)->{
            var player=context.player();var s=sessions.computeIfAbsent(player.getUUID(),id->new Session());int now=player.level().getServer().getTickCount();String result;
            if(now-s.lastAction<cooldown){reply(player,s,"cooldown");return;}s.lastAction=now;
            if(s.pending||a.serial()!=s.serial||a.token()!=s.token){reply(player,s,"changed");return;}s.token=++token;
            try{
                if(wallets==null) result="unavailable";
                else if(!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu) result="invalid";
                else result=switch(a.operation()){case 0->CoinEngine.withdraw(wallets,player.getInventory(),player.getUUID(),a.denomination(),a.quantity());case 1->CoinEngine.deposit(wallets,player.getInventory(),player.getUUID(),a.denomination(),a.quantity(),false);case 2->CoinEngine.deposit(wallets,player.getInventory(),player.getUUID(),0,0,true);default->"invalid";};
                if(result.equals("withdrawn")||result.equals("deposited")){player.inventoryMenu.broadcastChanges();player.level().getServer().getPlayerList().save(player);}
            }catch(Exception e){BalancedShop.LOG.error("Coin transfer failed",e);result="io";}reply(player,s,result);
        });
    }
    private static void reply(ServerPlayer player,Session s,String message){if(!ServerPlayNetworking.canSend(player,CoinPackets.Page.TYPE)) return;
        try{s.token=++token;List<Integer> counts=new ArrayList<>(4);for(int i=0;i<4;i++) counts.add(CoinItems.count(player.getInventory(),i));ServerPlayNetworking.send(player,new CoinPackets.Page(s.serial,s.token,wallets==null?0:wallets.balance(player.getUUID()),CoinItems.total(player.getInventory()),counts,wallets==null?"unavailable":message));}
        catch(Exception e){BalancedShop.LOG.error("Coin preview failed",e);ServerPlayNetworking.send(player,new CoinPackets.Page(s.serial,++token,0,0,List.of(0,0,0,0),"io"));}
    }
}
