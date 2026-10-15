package dev.eymen.balancedshop.client;

import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class BookScreen extends MarketScreen {
    private int x,y,w,h,rowHeight,listWidth,side,sideWidth,page,serial,quantity=1,debounce=-1,ticks;
    private String query="",message="loading";
    private boolean waiting,loading;
    private BookPackets.Page data;
    private BookCatalogue.Entry selected;
    private EditBox search;
    private Button previous,next,buy,amount;
    private final List<Button> rows=new ArrayList<>();
    public BookScreen(){super(Component.translatable("books.balancedshop.title"));}
    private Component text(String key,Object...args){return Component.translatable("books.balancedshop."+key,args);}
    @Override protected void init(){
        w=Math.min(540,width-8);h=Math.min(350,height-8);x=(width-w)/2;y=(height-h)/2;
        rowHeight=Math.max(17,Math.min(30,(h-128)/BookCatalogue.PAGE_SIZE));listWidth=w-140;side=x+listWidth+20;sideWidth=w-listWidth-30;
        search=addRenderableWidget(new EditBox(font,x+10,y+42,w-20,20,text("search")));search.setMaxLength(64);search.setValue(query);search.setHint(text("search"));
        search.setResponder(value->{query=value;page=0;debounce=6;});rows.clear();
        for(int i=0;i<BookCatalogue.PAGE_SIZE;i++){
            int index=i;rows.add(addRenderableWidget(ShopTheme.builder(Component.empty(),button->{
                if(data!=null&&index<data.entries().size()){selected=data.entries().get(index);refresh();}
            }).bounds(x+10,y+70+i*rowHeight,listWidth,rowHeight-1).build()));
        }
        amount=addRenderableWidget(ShopTheme.builder(Component.empty(),button->{quantity=quantity==1?16:quantity==16?32:quantity==32?64:1;refresh();}).bounds(side+22,y+119,sideWidth-44,20).build());
        addRenderableWidget(ShopTheme.button(Component.literal("-"),b->{quantity=Math.max(1,quantity-1);refresh();},side,y+119,20,20));
        addRenderableWidget(ShopTheme.button(Component.literal("+"),b->{quantity=Math.min(64,quantity+1);refresh();},side+sideWidth-20,y+119,20,20));
        buy=addRenderableWidget(ShopTheme.builder(Component.translatable("shop.balancedshop.buy"),button->purchase()).bounds(side,y+143,sideWidth,20).build());
        previous=addRenderableWidget(ShopTheme.builder(Component.literal("<"),button->{page--;request();}).bounds(x+10,y+h-28,24,20).build());
        next=addRenderableWidget(ShopTheme.builder(Component.literal(">"),button->{page++;request();}).bounds(x+38,y+h-28,24,20).build());
        addRenderableWidget(ShopTheme.builder(text("back"),button->ShopClient.open()).bounds(x+w-106,y+h-28,96,20).build());
        addRenderableWidget(ShopTheme.button(Component.translatable("coins.balancedshop.open"),b->ShopClient.openCoins(this),x+w-78,y+25,68,12));setInitialFocus(search);refresh();request();
    }
    private void request(){serial=ShopClient.nextSerial();loading=true;ticks=0;refresh();ClientPlayNetworking.send(new BookPackets.Query(serial,Math.max(0,page),query));}
    public void accept(BookPackets.Page response){
        if(response.serial()!=serial) return;data=response;page=response.page();waiting=false;loading=false;ticks=0;
        if(!response.message().isEmpty()) message=response.message();else if(message.equals("loading")) message="hint";
        if(selected!=null) selected=response.entries().stream().filter(e->e.id().equals(selected.id())&&e.level()==selected.level()).findFirst().orElse(null);
        refresh();
    }
    private void refresh(){
        if(buy==null) return;boolean ready=data!=null&&!waiting&&!loading;
        for(int i=0;i<rows.size();i++){rows.get(i).visible=data!=null&&i<data.entries().size();rows.get(i).active=ready;}
        previous.active=ready&&page>0;next.active=ready&&(page+1)*BookCatalogue.PAGE_SIZE<data.total();
        amount.active=ready;amount.setMessage(Component.translatable("shop.balancedshop.quantity",quantity));
        buy.active=ready&&selected!=null&&data.balance()>=Money.total(selected.buy(),quantity);
    }
    private void purchase(){
        if(!buy.active||selected==null) return;waiting=true;ticks=0;message="pending";refresh();
        ClientPlayNetworking.send(new BookPackets.Buy(serial,data.generation(),selected.id(),selected.level(),quantity));
    }
    @Override public void tick(){
        if(debounce>=0&&--debounce==0){debounce=-1;request();}
        if((waiting||loading)&&++ticks>100){waiting=false;message="timeout";request();}
        if(minecraft.player==null) onClose();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ShopTheme.panel(g,width,height,x,y,w,h,ShopTheme.VIOLET);
        g.drawString(font,title,x+10,y+10,0xFFFFFFFF);
        if(data!=null) g.drawString(font,Component.translatable("shop.balancedshop.balance",Money.format(data.balance())),x+10,y+26,0xFFAEDC86);
        ShopTheme.card(g,side-3,y+68,sideWidth+6,Math.min(h-124,112));
        super.render(g,mx,my,delta);
        if(data!=null) for(int i=0;i<data.entries().size();i++){
            var entry=data.entries().get(i);var row=rows.get(i);int rx=row.getX(),ry=row.getY();
            if(selected!=null&&selected.id().equals(entry.id())&&selected.level()==entry.level()) g.renderOutline(rx,ry,listWidth,rowHeight-1,0xFFECC772);
            g.renderFakeItem(entry.book(),rx+2,ry+(rowHeight-16)/2);
            String label=BookCatalogue.name(entry.book()).getString();
            if(rowHeight>=26){
                g.drawString(font,font.plainSubstrByWidth(label,listWidth-24),rx+22,ry+3,0xFFFFFFFF);
                g.drawString(font,Money.format(entry.buy())+" coin",rx+22,ry+15,0xFFAEDC86);
            }else g.drawString(font,font.plainSubstrByWidth(label,listWidth-24),rx+22,ry+4,0xFFFFFFFF);
            if(row.isHovered()) g.setTooltipForNextFrame(font,List.of(BookCatalogue.name(entry.book()),Component.literal(entry.id()),
                Component.translatable("shop.balancedshop.buyprice",Money.format(entry.buy()))),Optional.empty(),mx,my);
        }
        if(selected!=null){
            g.renderFakeItem(selected.book(),side+sideWidth/2-8,y+74);
            g.drawString(font,font.plainSubstrByWidth(BookCatalogue.name(selected.book()).getString(),sideWidth),side,y+97,0xFFFFFFFF);
            g.drawString(font,font.plainSubstrByWidth(Money.format(Money.total(selected.buy(),quantity))+" coin",sideWidth),side,y+170,0xFFAEDC86);
            if(mx>=side&&mx<side+sideWidth&&my>=y+70&&my<y+116) g.setTooltipForNextFrame(font,List.of(BookCatalogue.name(selected.book()),Component.literal(selected.id())),Optional.empty(),mx,my);
        }else g.drawString(font,font.plainSubstrByWidth(text("select").getString(),sideWidth),side,y+78,0xFFD2D9BF);
        String notice=Set.of("loading","hint","changed").contains(message)?text(message).getString():Component.translatable("shop.balancedshop."+message).getString();
        g.drawString(font,font.plainSubstrByWidth(notice,w-20),x+10,y+h-46,0xFFFFD590);
        if(data!=null) g.drawCenteredString(font,text("page",page+1,Math.max(1,(data.total()+BookCatalogue.PAGE_SIZE-1)/BookCatalogue.PAGE_SIZE),data.total()),x+w/2,y+h-22,0xFFD2D9BF);
    }
    @Override public boolean isPauseScreen(){return false;}
}
