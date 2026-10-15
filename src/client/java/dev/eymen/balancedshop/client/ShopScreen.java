package dev.eymen.balancedshop.client;

import dev.eymen.balancedshop.Catalogue;
import dev.eymen.balancedshop.Money;
import dev.eymen.balancedshop.ShopPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ShopScreen extends MarketScreen {
    private int x,y,panelWidth,panelHeight,cell,sideX,sideWidth;
    private EditBox search;
    private Button previous,next,filter,buy,sell,amount;
    private final List<Button> slots=new ArrayList<>();
    private ShopPackets.Page data;
    private ShopPackets.Entry selected;
    private String namespace="",searchValue="",message="loading";
    private int page,serial,quantity=1,debounce=-1,waitingTicks;
    private boolean waiting;
    public ShopScreen() { this(""); }
    public ShopScreen(String category){super(Component.translatable("shop.balancedshop.title"));namespace=category;}
    private MutableComponent text(String key,Object...args) { return Component.translatable("shop.balancedshop."+key,args); }
    @Override protected void init() {
        panelWidth=Math.min(540,width-8);panelHeight=Math.min(350,height-8);
        x=(width-panelWidth)/2;y=(height-panelHeight)/2;
        addRenderableWidget(ShopTheme.builder(Component.translatable("upgrade.balancedshop.open"),button->ShopClient.openUpgrade()).bounds(x+panelWidth-280,y+8,66,20).build());
        addRenderableWidget(ShopTheme.builder(Component.translatable("books.balancedshop.open"),button->ShopClient.openBooks())
            .bounds(x+panelWidth-208,y+8,96,20).build());
        addRenderableWidget(ShopTheme.builder(Component.translatable("sell.balancedshop.open"),button->ShopClient.openSell())
            .bounds(x+panelWidth-106,y+8,96,20).build());
        cell=Math.max(16,Math.min(32,Math.min((panelWidth-126)/9,(panelHeight-156)/5)));
        sideX=x+12+cell*9;sideWidth=x+panelWidth-10-sideX;
        search=addRenderableWidget(new EditBox(font,x+10,y+42,panelWidth-132,20,text("search")));
        search.setMaxLength(64);search.setHint(text("search"));search.setValue(searchValue);
        search.setResponder(value->{searchValue=value;page=0;debounce=6;});
        filter=addRenderableWidget(ShopTheme.builder(filterLabel(),button->cycleNamespace()).bounds(x+panelWidth-116,y+42,106,20).build());
        String[] filters={"","@farm","@mine","@tools","@blocks","create"};String[] labels={"all","farm","mine","tools","blocks","create"};
        int fw=(panelWidth-20)/6;
        for(int i=0;i<filters.length;i++){final String f=filters[i];addRenderableWidget(ShopTheme.button(Component.translatable("market.balancedshop."+labels[i]),b->{namespace=f;page=0;selected=null;filter.setMessage(filterLabel());request();},x+10+i*fw,y+68,fw-2,18));}
        slots.clear();
        for(int i=0;i<Catalogue.PAGE_SIZE;i++) {
            final int index=i;
            Button slot=addRenderableWidget(ShopTheme.builder(Component.empty(),button->{
                if(data!=null && index<data.entries().size()) {
                    if(data.entries().get(index).id().equals("minecraft:enchanted_book")){ShopClient.openBooks();return;}
                    selected=data.entries().get(index); updateActions();
                }
            }).bounds(x+10+(i%9)*cell,y+94+(i/9)*cell,cell-1,cell-1).build());
            slots.add(slot);
        }
        int controlsY=panelHeight<294?116:148;
        amount=addRenderableWidget(ShopTheme.builder(text("quantity",quantity),button->{
            quantity=quantity==1?16:quantity==16?32:quantity==32?64:1;amount.setMessage(text("quantity",quantity));updateActions();
        }).bounds(sideX+22,y+controlsY,sideWidth-44,20).build());
        addRenderableWidget(ShopTheme.button(Component.literal("-"),b->{quantity=Math.max(1,quantity-1);amount.setMessage(text("quantity",quantity));updateActions();},sideX,y+controlsY,20,20));
        addRenderableWidget(ShopTheme.button(Component.literal("+"),b->{quantity=Math.min(64,quantity+1);amount.setMessage(text("quantity",quantity));updateActions();},sideX+sideWidth-20,y+controlsY,20,20));
        int[] presets={1,16,32,64};for(int i=0;i<4;i++){int q=presets[i];addRenderableWidget(ShopTheme.button(Component.literal(""+q),b->{quantity=q;amount.setMessage(text("quantity",quantity));updateActions();},sideX+i*sideWidth/4,y+controlsY+22,sideWidth/4-1,16));}
        buy=addRenderableWidget(ShopTheme.builder(text("buy"),button->trade(false)).bounds(sideX,y+controlsY+42,sideWidth,20).build());
        sell=addRenderableWidget(ShopTheme.builder(text("sell"),button->ShopClient.openSell()).bounds(sideX,y+controlsY+64,sideWidth,20).build());
        previous=addRenderableWidget(ShopTheme.builder(Component.literal("<"),button->{page--;request();}).bounds(x+10,y+panelHeight-28,26,20).build());
        next=addRenderableWidget(ShopTheme.builder(Component.literal(">"),button->{page++;request();}).bounds(x+panelWidth-82,y+panelHeight-28,26,20).build());
        addRenderableWidget(ShopTheme.builder(Component.literal("X"),button->onClose()).bounds(x+panelWidth-36,y+panelHeight-28,26,20).build());
        addRenderableWidget(ShopTheme.button(Component.translatable("coins.balancedshop.open"),b->ShopClient.openCoins(this),x+panelWidth-78,y+25,68,12));setInitialFocus(search);
        refreshSlots();request();
    }
    private Component filterLabel() { return namespace.isEmpty()?text("allmods"):Component.literal(namespace); }
    private void cycleNamespace() {
        List<String> mods=data==null?List.of():data.namespaces();
        int index=mods.indexOf(namespace)+1;
        namespace=index>=mods.size()?"":mods.get(index);page=0;filter.setMessage(filterLabel());request();
    }
    private void request() {
        if(!ClientPlayNetworking.canSend(ShopPackets.Query.TYPE)) { message="missing";return; }
        serial=ShopClient.nextSerial();
        ClientPlayNetworking.send(new ShopPackets.Query(serial,Math.max(0,page),searchValue,namespace));
    }
    public void accept(ShopPackets.Page response) {
        if(response.serial()!=serial) return;
        data=response;page=response.page();waiting=false;waitingTicks=0;
        if(!response.message().isEmpty()) message=response.message(); else if(message.equals("loading")) message="hint";
        if(selected!=null) {
            selected=response.entries().stream().filter(e->e.id().equals(selected.id())).findFirst().orElse(null);
        }
        refreshSlots();
    }
    private void refreshSlots() {
        for(int i=0;i<slots.size();i++) { slots.get(i).visible=data!=null && i<data.entries().size(); }
        updateActions();
    }
    private void updateActions() {
        if(buy==null) return;
        boolean ready=data!=null && selected!=null && !waiting;
        buy.active=ready && selected.buyable() && data.balance()>=Money.total(selected.buy(),quantity);
        sell.active=!waiting;amount.active=!waiting;
        previous.active=data!=null && page>0 && !waiting;
        next.active=data!=null && (page+1)*Catalogue.PAGE_SIZE<data.total() && !waiting;
        filter.active=data!=null && !waiting;
    }
    private void trade(boolean selling) {
        if(selected==null||waiting) return;
        waiting=true;waitingTicks=0;message="pending";updateActions();
        ClientPlayNetworking.send(new ShopPackets.Trade(selected.id(),quantity,selling));
    }
    @Override public void tick() {
        if(debounce>=0 && --debounce==0) { debounce=-1;request(); }
        if(waiting && ++waitingTicks>100) { waiting=false;message="timeout";updateActions();request(); }
        if(minecraft.player==null) onClose();
    }
    private ItemStack stack(ShopPackets.Entry entry) {
        Identifier id=Identifier.tryParse(entry.id());
        return id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        ShopTheme.panel(g,width,height,x,y,panelWidth,panelHeight,ShopTheme.TEAL);
        g.drawString(font,font.plainSubstrByWidth(title.getString(),panelWidth-300),x+10,y+10,0xFFFFFFFF);
        if(data!=null) g.drawString(font,text("balance",Money.format(data.balance())),x+10,y+26,0xFF80E8CE);
        super.render(g,mouseX,mouseY,delta);
        if(data!=null) for(int i=0;i<data.entries().size() && i<slots.size();i++) {
            ShopPackets.Entry entry=data.entries().get(i);Button button=slots.get(i);
            int bx=button.getX(),by=button.getY();
            if(selected!=null && selected.id().equals(entry.id())) g.renderOutline(bx,by,cell-1,cell-1,0xFF53CDBA);
            ItemStack item=stack(entry);
            g.renderFakeItem(item,bx+(cell-16)/2,by+(cell-16)/2);
            if(!entry.buyable()&&!entry.id().equals("minecraft:enchanted_book")) g.fill(bx,by,bx+cell-1,by+cell-1,0x66000000);
            if(button.isHovered()) {
                List<Component> tooltip=new ArrayList<>();
                tooltip.add(item.getHoverName());tooltip.add(Component.literal(entry.id()).withColor(0x999999));
                if(entry.id().equals("minecraft:enchanted_book")) tooltip.add(Component.translatable("books.balancedshop.open"));
                else {
                tooltip.add(text("buyprice",Money.format(entry.buy())));
                tooltip.add(text("sellprice",Money.format(entry.sell())));
                if(!entry.buyable()) tooltip.add(text("restricted").withColor(0xFF9999));
                if(!entry.sellable()) tooltip.add(text("nosell").withColor(0xFFCC88));
                }
                g.setTooltipForNextFrame(font,tooltip,Optional.empty(),mouseX,mouseY);
            }
        }
        if(selected!=null) {
            ItemStack item=stack(selected);
            g.renderFakeItem(item,sideX+2,y+94);
            g.drawString(font,font.plainSubstrByWidth(item.getHoverName().getString(),sideWidth-22),sideX+22,y+98,0xFFFFFFFF);
            if(panelHeight>=294) {
                g.drawCenteredString(font,namespaceOf(selected.id()),sideX+sideWidth/2,y+126,0xFF9DAFC5);
                g.drawString(font,font.plainSubstrByWidth(text("buytotal",Money.format(Money.total(selected.buy(),quantity))).getString(),sideWidth),sideX,y+230,0xFF80E8CE);
                g.drawString(font,font.plainSubstrByWidth(text("selltotal",Money.format(Money.total(selected.sell(),quantity))).getString(),sideWidth),sideX,y+244,0xFFFFD590);
            }
            if(buy.isHovered()) g.setTooltipForNextFrame(text("buytotal",Money.format(Money.total(selected.buy(),quantity))),mouseX,mouseY);
            if(sell.isHovered()) g.setTooltipForNextFrame(text("selltotal",Money.format(Money.total(selected.sell(),quantity))),mouseX,mouseY);
        } else g.drawString(font,font.plainSubstrByWidth(text("select").getString(),sideWidth),sideX,y+84,0xFF9DAFC5);
        int footer=y+panelHeight-50;
        if(data!=null && data.entries().isEmpty()) g.drawString(font,text("empty"),x+12,y+84,0xFFBCCADA);
        String notice=text(message).getString();
        g.drawString(font,font.plainSubstrByWidth(notice,sideX-x-16),x+10,footer,0xFFFFD590);
        g.drawString(font,font.plainSubstrByWidth(Component.translatable("market.balancedshop.cart_hint").getString(),sideX-x-16),x+10,footer+14,0xFF9DAFC5);
        if(data!=null) g.drawCenteredString(font,text("page",page+1,Math.max(1,(data.total()+44)/45),data.total()),x+panelWidth/2,y+panelHeight-22,0xFFBCCADA);
    }
    private String namespaceOf(String id) { return id.substring(0,id.indexOf(':')); }
    @Override public boolean isPauseScreen() { return false; }
}
