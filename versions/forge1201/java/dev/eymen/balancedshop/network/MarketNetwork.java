package dev.eymen.balancedshop.network;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.command.EconomyCommands;
import dev.eymen.balancedshop.data.DynamicMarket;
import dev.eymen.balancedshop.menu.ShopMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Consumer;

/** Only item identifiers and intent cross C2S. Prices, ownership, balance and stock are never trusted from clients. */
public final class MarketNetwork {
    private static final String VERSION="2";
    public static final int MIN_PAGE_SIZE=9,MAX_PAGE_SIZE=30;
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("balancedshop","market_21"),()->VERSION,VERSION::equals,VERSION::equals);
    public static Consumer<Page> receiver=page->{}; // Installed only by the physical client; safe on dedicated servers.
    private static long tooltipRevision=-1;
    public record Open(){}
    public record Request(int container,UUID session,long sequence,int operation,String item,int quantity,String search,String category,int page,long revision,int pageSize) {
        public Request(int container,UUID session,long sequence,int operation,String item,int quantity,String search,String category,int page,long revision){this(container,session,sequence,operation,item,quantity,search,category,page,revision,MIN_PAGE_SIZE);}
        static Request decode(FriendlyByteBuf b){return new Request(b.readVarInt(),b.readUUID(),b.readLong(),b.readUnsignedByte(),b.readUtf(256),b.readVarInt(),b.readUtf(64),b.readUtf(64),b.readVarInt(),b.readLong(),b.readVarInt());}
        void encode(FriendlyByteBuf b){b.writeVarInt(container);b.writeUUID(session);b.writeLong(sequence);b.writeByte(operation);b.writeUtf(item,256);b.writeVarInt(quantity);b.writeUtf(search,64);b.writeUtf(category,64);b.writeVarInt(page);b.writeLong(revision);b.writeVarInt(pageSize);}
    }
    public record Row(String id,long buy,long sell,long stock,long previous){}
    public record Page(int container,UUID session,long sequence,long revision,int page,int total,long balance,List<Row> rows,String selected,long base,long min,long max,DynamicMarket.Quote buy,DynamicMarket.Quote sell,String message){
        static Page decode(FriendlyByteBuf b){
            int container=b.readVarInt();UUID session=b.readUUID();long seq=b.readLong(),revision=b.readLong();int page=b.readVarInt(),total=b.readVarInt();long balance=b.readLong();
            int size=b.readVarInt();if(size<0||size>MAX_PAGE_SIZE)throw new IllegalArgumentException("Market page size");List<Row> rows=new ArrayList<>();
            for(int i=0;i<size;i++)rows.add(new Row(b.readUtf(256),b.readLong(),b.readLong(),b.readLong(),b.readLong()));
            return new Page(container,session,seq,revision,page,total,balance,List.copyOf(rows),b.readUtf(256),b.readLong(),b.readLong(),b.readLong(),readQuote(b),readQuote(b),b.readUtf(256));
        }
        void encode(FriendlyByteBuf b){
            b.writeVarInt(container);b.writeUUID(session);b.writeLong(sequence);b.writeLong(revision);b.writeVarInt(page);b.writeVarInt(total);b.writeLong(balance);b.writeVarInt(rows.size());
            for(var r:rows){b.writeUtf(r.id(),256);b.writeLong(r.buy());b.writeLong(r.sell());b.writeLong(r.stock());b.writeLong(r.previous());}
            b.writeUtf(selected,256);b.writeLong(base);b.writeLong(min);b.writeLong(max);writeQuote(b,buy);writeQuote(b,sell);b.writeUtf(message,256);
        }
    }
    private static DynamicMarket.Quote readQuote(FriendlyByteBuf b){return new DynamicMarket.Quote(b.readVarInt(),b.readLong(),b.readLong(),b.readUtf(32));}
    private static void writeQuote(FriendlyByteBuf b,DynamicMarket.Quote q){b.writeVarInt(q.quantity());b.writeLong(q.total());b.writeLong(q.afterStock());b.writeUtf(q.message(),32);}
    public static void install(IEventBus bus){
        ShopMenu.register(bus);MinecraftForge.EVENT_BUS.addListener(EconomyCommands::register);
        CHANNEL.messageBuilder(Open.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{}).decoder(b->new Open()).consumerMainThread((p,c)->open(c.get().getSender())).add();
        CHANNEL.messageBuilder(Request.class,1,NetworkDirection.PLAY_TO_SERVER).encoder(Request::encode).decoder(Request::decode).consumerMainThread((p,c)->handle(c.get().getSender(),p)).add();
        CHANNEL.messageBuilder(Page.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(Page::encode).decoder(Page::decode).consumerMainThread((p,c)->receiver.accept(p)).add();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e)->{
            if(e.phase!=TickEvent.Phase.END||e.getServer().getTickCount()%20!=0)return;
            var catalogue=BalancedShop.catalogue();if(catalogue==null||catalogue.market()==null)return;
            long revision=catalogue.market().data().revision();
            if(tooltipRevision!=revision){tooltipRevision=revision;PriceSync.configure(catalogue,e.getServer());}
        });
    }
    public static void open(ServerPlayer player){
        if(player==null||BalancedShop.catalogue()==null||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu)return;
        UUID session=UUID.randomUUID();
        NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new ShopMenu(id,inv,session),Component.literal("Balanced Shop")),buf->buf.writeUUID(session));
    }
    public static boolean valid(ShopMenu menu,Request r){return r.container()==menu.containerId&&r.session().equals(menu.session)&&r.sequence()>menu.sequence&&r.operation()>=0&&r.operation()<=2&&r.quantity()>=0&&r.quantity()<=DynamicMarket.MAX_QUANTITY&&r.page()>=0&&r.page()<=1_000_000&&r.pageSize()>=MIN_PAGE_SIZE&&r.pageSize()<=MAX_PAGE_SIZE;}
    private static void handle(ServerPlayer player,Request r){
        if(player==null||!(player.containerMenu instanceof ShopMenu menu)||!menu.stillValid(player)||!valid(menu,r))return;
        var catalogue=BalancedShop.catalogue();var wallets=BalancedShop.wallets();if(catalogue==null||wallets==null)return;
        int now=player.server.getTickCount();
        // A quote request must not swallow the following legitimate trade click.
        if(r.operation()==0&&now-menu.lastRequest<2)return;
        if(r.operation()!=0&&now-menu.lastTrade<4&&now-menu.lastRequest<2)return;
        menu.lastRequest=now;menu.sequence=r.sequence();
        String message="";
        try {
            var market=catalogue.market();
            if(r.operation()!=0){
                if(now-menu.lastTrade<4)message="cooldown";
                else {
                    menu.lastTrade=now;
                    var fresh=market.quote(wallets,player.getInventory(),player.getUUID(),r.item(),r.quantity(),r.operation()==2);
                    // Compare the server-held quote, including resolved Max quantity. A /pay, pickup or
                    // another trade cannot silently change the displayed order. Unrelated stock may change safely.
                    if(menu.preview==null||!menu.preview.matches(r.item(),r.quantity(),r.revision(),r.operation()==2,fresh))message="changed";
                    else {
                        var result=market.execute(wallets,player.getInventory(),player.getUUID(),r.item(),r.quantity(),r.operation()==2);
                        message=result.message();player.inventoryMenu.sendAllDataToRemote();
                    }
                }
            }
            var found=catalogue.search(r.search(),r.category());int page=Math.min(r.page(),Math.max(0,(found.size()-1)/r.pageSize())),start=page*r.pageSize();
            List<Row> rows=new ArrayList<>();for(var e:found.subList(start,Math.min(start+r.pageSize(),found.size())))rows.add(new Row(e.id(),e.buy(),e.sell(),market.definition(e.id()).stock(),market.previousPrice(e.id())));
            boolean selected=catalogue.baseEntry(r.item())!=null;
            var none=new DynamicMarket.Quote(0,0,0,"invalid");
            var buy=selected?market.quote(wallets,player.getInventory(),player.getUUID(),r.item(),r.quantity(),false):none;
            var sell=selected?market.quote(wallets,player.getInventory(),player.getUUID(),r.item(),r.quantity(),true):none;
            var definition=selected?market.definition(r.item()):new DynamicMarket.Definition(0,0,0,0);
            menu.preview=new ShopMenu.Preview(r.item(),r.quantity(),market.data().revision(),buy,sell);
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Page(menu.containerId,menu.session,r.sequence(),market.data().revision(),page,found.size(),wallets.balance(player.getUUID()),List.copyOf(rows),r.item(),definition.base(),definition.min(),definition.max(),buy,sell,message));
        }catch(Exception e){BalancedShop.LOG.error("Market transaction failed for {}",player.getUUID(),e);player.sendSystemMessage(Component.literal("Dükkan işlemi tamamlanamadı; sunucu günlüğüne bakın."));}
    }
}
