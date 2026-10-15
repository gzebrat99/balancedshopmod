package dev.eymen.balancedshop;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import java.util.*;

/** All authorization and payments happen on the server; the GUI is only a catalogue. */
public final class CreateTraderService {
    private static Catalogue catalogue;
    private static boolean enabled=true;
    private static int cooldown=4;
    private static long sequence;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final List<String> FEATURED=List.of("create:shaft","create:andesite_alloy","create:cogwheel","create:large_cogwheel","create:gearbox","create:mechanical_press");
    private static final class Session {
        UUID villager; int serial,page,lastQuery=Integer.MIN_VALUE/2,lastBuy=Integer.MIN_VALUE/2;
        String search="";long token;boolean pending;
        Session(UUID villager){this.villager=villager;}
    }
    public static void configure(Catalogue c,ShopConfig config){catalogue=c;enabled=config.createVillagersEnabled;cooldown=config.transactionCooldownTicks;sessions.clear();}
    public static void clear(){catalogue=null;sessions.clear();}
    public static boolean replaces(Villager villager){return enabled&&villager.getVillagerData().profession().is(VillagerProfession.WEAPONSMITH);}
    public static List<ShopPackets.Entry> offers(Catalogue c,String search){
        return c.search(search,"create").stream().filter(ShopPackets.Entry::buyable)
            .sorted(Comparator.comparingInt((ShopPackets.Entry e)->{int i=FEATURED.indexOf(e.id());return i<0?100:i;}).thenComparing(ShopPackets.Entry::id)).toList();
    }
    private static boolean valid(ServerPlayer player,Session s){
        if(s==null||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu) return false;
        var entity=player.level().getEntity(s.villager);
        return entity instanceof Villager v&&replaces(v)&&v.isAlive()&&!v.isBaby()&&!v.isSleeping()&&!v.isTrading()&&player.distanceToSqr(v)<=36;
    }
    public static void register(){
        PayloadTypeRegistry.playC2S().register(TraderPackets.Query.TYPE,TraderPackets.Query.CODEC);
        PayloadTypeRegistry.playC2S().register(TraderPackets.Buy.TYPE,TraderPackets.Buy.CODEC);
        PayloadTypeRegistry.playS2C().register(TraderPackets.Open.TYPE,TraderPackets.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(TraderPackets.Page.TYPE,TraderPackets.Page.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->sessions.remove(h.getPlayer().getUUID()));
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            if(!(entity instanceof Villager villager)||!replaces(villager)||player.isSpectator()||player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)) return InteractionResult.PASS;
            if(villager.isBaby()||!villager.isAlive()||villager.isSleeping()||villager.isTrading()) return InteractionResult.PASS;
            if(!level.isClientSide()&&player instanceof ServerPlayer serverPlayer){
                if(catalogue!=null&&ServerPlayNetworking.canSend(serverPlayer,TraderPackets.Open.TYPE)){
                    var s=new Session(villager.getUUID());sessions.put(player.getUUID(),s);
                    if(valid(serverPlayer,s)) ServerPlayNetworking.send(serverPlayer,new TraderPackets.Open());
                }
            }
            return InteractionResult.SUCCESS;
        });
        ServerPlayNetworking.registerGlobalReceiver(TraderPackets.Query.TYPE,(q,context)->{
            var s=sessions.get(context.player().getUUID());if(s==null){closed(context.player(),q.serial());return;}
            s.serial=q.serial();s.page=Math.max(0,q.page());s.search=q.search().strip();s.token=++sequence;s.pending=true;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var entry:sessions.entrySet()){
            var s=entry.getValue();if(s.pending&&server.getTickCount()-s.lastQuery>=2){s.pending=false;s.lastQuery=server.getTickCount();var player=server.getPlayerList().getPlayer(entry.getKey());if(player!=null) reply(player,s,"");}
        }});
        ServerPlayNetworking.registerGlobalReceiver(TraderPackets.Buy.TYPE,(buy,context)->{
            var player=context.player();var s=sessions.get(player.getUUID());if(s==null){closed(player,buy.serial());return;}
            int now=player.level().getServer().getTickCount();String result;
            if(!valid(player,s)){closed(player,s.serial);sessions.remove(player.getUUID());return;}
            if(now-s.lastBuy<cooldown){reply(player,s,"cooldown");return;}s.lastBuy=now;
            if(s.pending||buy.serial()!=s.serial||buy.token()!=s.token){reply(player,s,"changed");return;}s.token=++sequence;
            try{
                result=catalogue==null?"unavailable":CoinEngine.buyCreate(catalogue,player.getInventory(),buy.id(),buy.quantity());
                if(result.equals("bought")){player.inventoryMenu.broadcastChanges();player.level().getServer().getPlayerList().save(player);}
            }catch(Exception e){BalancedShop.LOG.error("Create villager trade failed",e);result="io";}
            reply(player,s,result);
        });
    }
    private static void closed(ServerPlayer p,int serial){if(ServerPlayNetworking.canSend(p,TraderPackets.Page.TYPE)) ServerPlayNetworking.send(p,new TraderPackets.Page(serial,0,0,0,CoinItems.total(p.getInventory()),"trader_closed",List.of()));}
    private static void reply(ServerPlayer p,Session s,String message){
        if(!valid(p,s)){closed(p,s.serial);return;}
        var list=catalogue==null?List.<ShopPackets.Entry>of():offers(catalogue,s.search);
        int page=Math.min(s.page,Math.max(0,(list.size()-1)/TraderPackets.PAGE_SIZE)),start=page*TraderPackets.PAGE_SIZE;
        s.token=++sequence;
        ServerPlayNetworking.send(p,new TraderPackets.Page(s.serial,s.token,page,list.size(),CoinItems.total(p.getInventory()),catalogue==null?"unavailable":message,List.copyOf(list.subList(start,Math.min(start+TraderPackets.PAGE_SIZE,list.size())))));
    }
}
