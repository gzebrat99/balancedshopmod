package dev.eymen.balancedshop.client;
import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class CreateTraderScreen extends MarketScreen {
    private int x,y,w,h,rowH,listW,side,sideW,page,serial,quantity=1,debounce=-1,ticks;
    private String query="",message="loading";
    private boolean waiting,loading,closed;
    private TraderPackets.Page data;private ShopPackets.Entry selected;
    private EditBox search;private Button previous,next,buy,minus,plus;
    private final List<Button> rows=new ArrayList<>(),presets=new ArrayList<>();
    public CreateTraderScreen(){super(Component.translatable("trader.balancedshop.title"));}
    private Component text(String key,Object...args){return Component.translatable("trader.balancedshop."+key,args);}
    private ItemStack stack(ShopPackets.Entry e){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(e.id())));}
    @Override protected void init(){
        w=Math.min(540,width-8);h=Math.min(350,height-8);x=(width-w)/2;y=(height-h)/2;
        rowH=Math.max(13,Math.min(24,(h-128)/TraderPackets.PAGE_SIZE));listW=w-150;side=x+listW+20;sideW=w-listW-30;
        addRenderableWidget(ShopTheme.button(Component.translatable("coins.balancedshop.open"),b->ShopClient.openCoins(this),x+w-78,y+24,68,13));
        search=addRenderableWidget(new EditBox(font,x+10,y+42,w-20,20,text("search")));search.setMaxLength(64);search.setValue(query);search.setHint(text("search"));search.setResponder(v->{query=v;page=0;selected=null;debounce=6;});rows.clear();
        for(int i=0;i<TraderPackets.PAGE_SIZE;i++){int index=i;rows.add(addRenderableWidget(ShopTheme.button(Component.empty(),b->{if(data!=null&&index<data.entries().size()){selected=data.entries().get(index);refresh();}},x+10,y+70+i*rowH,listW,rowH-1)));}
        minus=addRenderableWidget(ShopTheme.button(Component.literal("<"),b->{quantity=Math.max(1,quantity-1);refresh();},side,y+115,20,18));
        plus=addRenderableWidget(ShopTheme.button(Component.literal(">"),b->{quantity=Math.min(64,quantity+1);refresh();},side+sideW-20,y+115,20,18));
        presets.clear();int[] counts={1,16,32,64};for(int i=0;i<4;i++){int q=counts[i];presets.add(addRenderableWidget(ShopTheme.button(Component.literal(""+q),b->{quantity=q;refresh();},side+i*sideW/4,y+138,sideW/4-1,16)));}
        buy=addRenderableWidget(ShopTheme.button(Component.translatable("shop.balancedshop.buy"),b->purchase(),side,y+159,sideW,20));
        previous=addRenderableWidget(ShopTheme.button(Component.literal("<"),b->{page--;selected=null;request();},x+10,y+h-28,24,20));
        next=addRenderableWidget(ShopTheme.button(Component.literal(">"),b->{page++;selected=null;request();},x+38,y+h-28,24,20));
        addRenderableWidget(ShopTheme.button(Component.literal("X"),b->onClose(),x+w-36,y+h-28,26,20));
        setInitialFocus(search);refresh();request();
    }
    private void request(){serial=ShopClient.nextSerial();loading=true;ticks=0;refresh();ClientPlayNetworking.send(new TraderPackets.Query(serial,Math.max(0,page),query));}
    public void accept(TraderPackets.Page p){if(p.serial()!=serial)return;data=p;page=p.page();waiting=loading=false;closed=p.message().equals("trader_closed");ticks=0;message=p.message().isEmpty()?(p.total()==0?"empty":"hint"):p.message();
        if(selected!=null)selected=p.entries().stream().filter(e->e.id().equals(selected.id())).findFirst().orElse(null);refresh();}
    private void refresh(){if(buy==null)return;boolean ready=data!=null&&!waiting&&!loading&&!closed;for(int i=0;i<rows.size();i++){rows.get(i).visible=data!=null&&i<data.entries().size();rows.get(i).active=ready;}
        previous.active=ready&&page>0;next.active=ready&&(page+1)*TraderPackets.PAGE_SIZE<data.total();minus.active=ready&&quantity>1;plus.active=ready&&quantity<64;presets.forEach(b->b.active=ready);buy.active=ready&&selected!=null&&data.physical()>=Money.total(selected.buy(),quantity);}
    private void purchase(){if(!buy.active)return;waiting=true;ticks=0;message="pending";refresh();ClientPlayNetworking.send(new TraderPackets.Buy(serial,data.token(),selected.id(),quantity));}
    @Override public void tick(){if(debounce>=0&&--debounce==0){debounce=-1;request();}if((waiting||loading)&&++ticks>100){message="timeout";request();}if(!waiting&&!loading&&++ticks>=40){ticks=0;request();}if(minecraft.player==null)onClose();}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ShopTheme.panel(g,width,height,x,y,w,h,ShopTheme.GOLD);g.drawString(font,title,x+10,y+10,ShopTheme.GOLD);
        if(data!=null)g.drawString(font,font.plainSubstrByWidth(text("physical",Money.format(data.physical())).getString(),w-100),x+10,y+26,ShopTheme.TEAL);
        super.render(g,mx,my,delta);
        if(data!=null)for(int i=0;i<data.entries().size();i++){var entry=data.entries().get(i);var row=rows.get(i);int rx=row.getX(),ry=row.getY();
            if(selected!=null&&selected.id().equals(entry.id()))g.renderOutline(rx,ry,listW,rowH-1,ShopTheme.GOLD);
            if(rowH>=17)g.renderFakeItem(stack(entry),rx+2,ry+(rowH-16)/2);
            int inset=rowH>=17?22:4;g.drawString(font,font.plainSubstrByWidth(stack(entry).getHoverName().getString(),listW-inset-8),rx+inset,ry+3,0xFFFFFFFF);
            if(rowH>=24)g.drawString(font,Money.format(entry.buy())+" coin",rx+inset,ry+14,ShopTheme.TEAL);
            if(row.isHovered())g.setTooltipForNextFrame(font,List.of(stack(entry).getHoverName(),Component.literal(entry.id()),Component.translatable("shop.balancedshop.buyprice",Money.format(entry.buy()))),Optional.empty(),mx,my);
        }
        if(selected!=null){ShopTheme.gear(g,side+sideW/2,y+83,15,ShopTheme.GOLD);g.renderFakeItem(stack(selected),side+sideW/2-8,y+75);g.drawString(font,font.plainSubstrByWidth(stack(selected).getHoverName().getString(),sideW),side,y+98,0xFFFFFFFF);}
        g.drawCenteredString(font,Component.literal(""+quantity),side+sideW/2,y+120,ShopTheme.GOLD);
        if(selected!=null&&h>260)g.drawString(font,font.plainSubstrByWidth(Money.format(Money.total(selected.buy(),quantity))+" coin",sideW),side,y+188,ShopTheme.TEAL);
        Component notice=Set.of("empty","hint","trader_closed","funds").contains(message)?text(message):Component.translatable("shop.balancedshop."+message);
        g.drawString(font,font.plainSubstrByWidth(notice.getString(),w-20),x+10,y+h-46,ShopTheme.GOLD);
        if(data!=null)g.drawCenteredString(font,text("page",page+1,Math.max(1,(data.total()+TraderPackets.PAGE_SIZE-1)/TraderPackets.PAGE_SIZE),data.total()),x+w/2,y+h-22,0xFFAABBD4);
    }
    @Override public boolean isPauseScreen(){return false;}
}
