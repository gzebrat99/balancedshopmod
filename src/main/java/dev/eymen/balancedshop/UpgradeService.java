package dev.eymen.balancedshop;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class UpgradeService {
    private static Catalogue catalogue;private static BookCatalogue books;private static WalletStore wallets;private static ShopConfig config;
    private static long nextToken;
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Session {
        UpgradePackets.Query query=new UpgradePackets.Query(0,0,false,0,"","");
        int lastQuery=Integer.MIN_VALUE/2,lastRoll=Integer.MIN_VALUE/2;boolean pending;long token;UpgradeEngine.Quote quote;
    }
    public static void configure(Catalogue c,BookCatalogue b,WalletStore w,ShopConfig cfg){catalogue=c;books=b;wallets=w;config=cfg;sessions.values().forEach(s->{s.quote=null;s.token=++nextToken;s.pending=true;});}
    public static void clear(){catalogue=null;books=null;wallets=null;config=null;sessions.clear();}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(UpgradePackets.Query.TYPE,UpgradePackets.Query.CODEC);
        PayloadTypeRegistry.playC2S().register(UpgradePackets.Roll.TYPE,UpgradePackets.Roll.CODEC);
        PayloadTypeRegistry.playS2C().register(UpgradePackets.Page.TYPE,UpgradePackets.Page.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->sessions.remove(handler.getPlayer().getUUID()));
        ServerPlayNetworking.registerGlobalReceiver(UpgradePackets.Query.TYPE,(query,context)->{
            var session=sessions.computeIfAbsent(context.player().getUUID(),id->new Session());session.query=query;session.quote=null;session.token=++nextToken;session.pending=true;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var entry:sessions.entrySet()){
            var s=entry.getValue();if(!s.pending||server.getTickCount()-s.lastQuery<2) continue;s.pending=false;s.lastQuery=server.getTickCount();
            var player=server.getPlayerList().getPlayer(entry.getKey());if(player!=null) reply(player,s,"");
        }});
        ServerPlayNetworking.registerGlobalReceiver(UpgradePackets.Roll.TYPE,(roll,context)->{
            var player=context.player();var s=sessions.computeIfAbsent(player.getUUID(),id->new Session());int now=player.level().getServer().getTickCount();String result;
            if(config==null){reply(player,s,"unavailable");return;}
            if(now-s.lastRoll<config.transactionCooldownTicks){reply(player,s,"cooldown");return;}s.lastRoll=now;
            var quote=s.quote;
            if(roll.serial()!=s.query.serial()||roll.token()!=s.token||quote==null||s.pending){reply(player,s,"changed");return;}
            s.quote=null;s.token=++nextToken; // Every quote is single-use, including rejected attempts.
            try{
                if(!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu) result="invalid";
                else result=UpgradeEngine.execute(quote,wallets,player.getInventory(),player.getUUID(),s.query.slot(),config.upgradeDestroyOnFailure,()->player.getRandom().nextDouble());
                if(result.equals("success")||result.equals("failed")){
                    player.inventoryMenu.broadcastChanges();player.level().getServer().getPlayerList().save(player);
                    BalancedShop.LOG.info("Upgrade player={} target={} give={} receive={} chance={} fee={} result={}",player.getUUID(),quote.target().key(),quote.give(),quote.receive(),quote.chance(),quote.fee(),result);
                    s.query=new UpgradePackets.Query(s.query.serial(),s.query.slot(),s.query.enchant(),s.query.page(),s.query.search(),"",s.query.give(),s.query.receive());
                }
            }catch(Exception e){BalancedShop.LOG.error("Upgrade failed",e);result="io";}
            reply(player,s,result);
        });
    }
    private static void reply(ServerPlayer player,Session s,String message){
        if(!ServerPlayNetworking.canSend(player,UpgradePackets.Page.TYPE)) return;
        try{
            int slot=Math.max(0,Math.min(35,s.query.slot()));ItemStack source=player.getInventory().getItem(slot);
            int give=s.query.give(),receive=s.query.receive();boolean valid=give>=1&&give<=UpgradeEngine.MAX_QUANTITY&&receive>=1&&receive<=UpgradeEngine.MAX_QUANTITY;
            long unit=catalogue==null?0:UpgradeEngine.stakeValue(catalogue,source,s.query.enchant());
            long value=valid?unit*give:0;int available=UpgradeEngine.available(player.getInventory(),source);
            var found=catalogue==null?List.<UpgradeEngine.Target>of():UpgradeEngine.targets(catalogue,books,source,s.query.enchant(),s.query.search());
            int page=Math.max(0,Math.min(s.query.page(),Math.max(0,(found.size()-1)/UpgradePackets.PAGE_SIZE))),start=page*UpgradePackets.PAGE_SIZE;
            var target=found.stream().filter(t->t.key().equals(s.query.target())).findFirst().orElse(null);
            s.quote=target==null||!valid?null:UpgradeEngine.quote(player.getInventory(),slot,target,unit,give,receive,config.upgradeDestroyOnFailure);s.token=++nextToken;
            if(!valid) message="invalid";else if(target!=null&&available<give) message="items";
            List<ItemStack> inventory=new ArrayList<>(36);for(int i=0;i<36;i++) inventory.add(player.getInventory().getItem(i).copy());
            ServerPlayNetworking.send(player,new UpgradePackets.Page(s.query.serial(),s.token,slot,page,found.size(),wallets==null?0:wallets.balance(player.getUUID()),value,s.quote==null?0:s.quote.chance(),s.quote==null?0:s.quote.fee(),config==null||config.upgradeDestroyOnFailure,target==null?"":target.key(),catalogue==null?"unavailable":message,inventory,List.copyOf(found.subList(start,Math.min(start+UpgradePackets.PAGE_SIZE,found.size()))),give,receive,available));
        }catch(Exception e){s.quote=null;BalancedShop.LOG.error("Upgrade preview failed",e);List<ItemStack> empty=new ArrayList<>(Collections.nCopies(36,ItemStack.EMPTY));ServerPlayNetworking.send(player,new UpgradePackets.Page(s.query.serial(),++nextToken,0,0,0,0,0,0,0,true,"","io",empty,List.of(),1,1,0));}
    }
}
