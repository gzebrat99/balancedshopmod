package dev.eymen.balancedshop.client;

import dev.eymen.balancedshop.Money;
import dev.eymen.balancedshop.SellPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class SellScreen extends MarketScreen {
    private int x,y,w,h,cell,side,sideWidth,serial,ticks,quantity=1;
    private String selected="",message="loading";
    private boolean waiting,loading;
    private SellPackets.Page data;
    private Button customMinus,customPlus,customSell;
    private final List<Button> slots=new ArrayList<>(),options=new ArrayList<>();
    private final int[] amounts={0,1,16,32,64};
    public SellScreen(){super(Component.translatable("sell.balancedshop.title"));}
    private Component text(String key,Object...args){return Component.translatable("sell.balancedshop."+key,args);}
    @Override protected void init(){
        w=Math.min(500,width-8);h=Math.min(330,height-8);x=(width-w)/2;y=(height-h)/2;
        cell=Math.max(16,Math.min(30,Math.min((w-154)/9,(h-128)/4)));
        side=x+20+cell*9;sideWidth=x+w-10-side;slots.clear();options.clear();
        for(int i=0;i<36;i++){
            int slot=i<27?i+9:i-27;
            slots.add(addRenderableWidget(ShopTheme.builder(Component.empty(),button->{
                if(data==null) return;var entry=data.inventory().get(slot);
                selected=entry.id();message=selected.isEmpty()?"select":!entry.plain()?"special":!entry.sellable()?"restricted":"ready";refresh();
            }).bounds(x+10+(i%9)*cell,y+70+(i/9)*cell+(i>=27?4:0),cell-1,cell-1).build()));
        }
        for(int i=0;i<amounts.length;i++){
            int amount=amounts[i];
            options.add(addRenderableWidget(ShopTheme.builder(Component.empty(),button->sell(amount)).bounds(side+(i%2)*(sideWidth/2),y+70+(i/2)*22,sideWidth/2-2,20).build()));
        }
        customMinus=addRenderableWidget(ShopTheme.button(Component.literal("-"),b->{quantity=Math.max(1,quantity-1);refresh();},side,y+140,20,20));
        customPlus=addRenderableWidget(ShopTheme.button(Component.literal("+"),b->{quantity=Math.min(2304,quantity+1);refresh();},side+sideWidth-20,y+140,20,20));
        customSell=addRenderableWidget(ShopTheme.button(Component.translatable("shop.balancedshop.sell"),b->sell(quantity),side,y+164,sideWidth,20));
        addRenderableWidget(ShopTheme.builder(text("back"),button->ShopClient.open()).bounds(x+10,y+h-28,90,20).build());
        addRenderableWidget(ShopTheme.builder(Component.literal("X"),button->onClose()).bounds(x+w-36,y+h-28,26,20).build());
        addRenderableWidget(ShopTheme.button(Component.translatable("coins.balancedshop.open"),b->ShopClient.openCoins(this),x+w-78,y+25,68,12));refresh();request();
    }
    private void request(){serial=ShopClient.nextSerial();loading=true;ticks=0;refresh();ClientPlayNetworking.send(new SellPackets.Query(serial));}
    public void accept(SellPackets.Page page){
        if(page.serial()!=serial) return;data=page;waiting=false;loading=false;ticks=0;
        if(!page.message().isEmpty()) message=page.message();else if(message.equals("loading")) message="select";
        refresh();
    }
    private int available(){
        return data==null||selected.isEmpty()?0:data.inventory().stream().filter(s->s.id().equals(selected)&&s.plain()&&s.sellable()).mapToInt(SellPackets.Slot::count).sum();
    }
    private long price(){return data==null?0:data.inventory().stream().filter(s->s.id().equals(selected)).mapToLong(SellPackets.Slot::price).findFirst().orElse(0);}
    private long total(int quantity){long total=0;if(data!=null) for(var s:data.inventory()) if(quantity>0&&s.id().equals(selected)&&s.plain()&&s.sellable()){int take=Math.min(quantity,s.count());total+=take*s.price();quantity-=take;}return total;}
    private void refresh(){
        int count=available();boolean ready=data!=null&&!loading&&!waiting;
        if(customSell!=null){
            customMinus.visible=customPlus.visible=customSell.visible=!selected.isEmpty();
            customMinus.active=ready&&quantity>1;customPlus.active=ready&&quantity<count;
            customSell.active=ready&&quantity>0&&quantity<=count&&data.balance()<=Money.MAX-total(quantity);
            customSell.setMessage(text("amount",quantity));
        }
        for(Button slot:slots) slot.active=ready;
        for(int i=0;i<options.size();i++){
            int amount=amounts[i],quantity=amount==0?count:amount;Button button=options.get(i);
            button.visible=!selected.isEmpty();button.active=ready&&quantity>0&&count>=quantity&&!message.equals("special")
                &&data.balance()<=Money.MAX-total(quantity);
            button.setMessage(amount==0?text("all",count):text("amount",amount));
        }
    }
    private void sell(int amount){
        if(data==null||waiting||loading||selected.isEmpty()) return;
        waiting=true;ticks=0;message="pending";refresh();
        ClientPlayNetworking.send(new SellPackets.Action(serial,selected,amount));
    }
    @Override public void tick(){
        if((waiting||loading)&&++ticks>100){waiting=false;message="timeout";request();}
        if(minecraft.player==null) onClose();
    }
    private ItemStack stack(String id){if(data!=null)for(var slot:data.inventory())if(slot.id().equals(id))return slot.stack();Identifier key=Identifier.tryParse(id);return key==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(key));}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ShopTheme.panel(g,width,height,x,y,w,h,ShopTheme.VIOLET);
        g.drawString(font,title,x+10,y+10,0xFFFFFFFF);
        if(data!=null) g.drawString(font,Component.translatable("shop.balancedshop.balance",Money.format(data.balance())),x+10,y+26,0xFFAEDC86);
        g.drawString(font,font.plainSubstrByWidth(text("hint").getString(),w-20),x+10,y+42,0xFFD2D9BF);
        g.drawString(font,text("inventory"),x+10,y+57,0xFFFFFFFF);
        super.render(g,mx,my,delta);
        if(data!=null) for(int i=0;i<36;i++){
            int slot=i<27?i+9:i-27;var entry=data.inventory().get(slot);Button button=slots.get(i);
            if(entry.id().isEmpty()) continue;
            ItemStack item=entry.stack();int ix=button.getX()+(cell-16)/2,iy=button.getY()+(cell-16)/2;
            g.renderFakeItem(item,ix,iy);g.renderItemDecorations(font,item,ix,iy,String.valueOf(entry.count()));
            if(entry.id().equals(selected)) g.renderOutline(button.getX(),button.getY(),cell-1,cell-1,0xFF8BDB77);
            if(!entry.plain()||!entry.sellable()) g.fill(button.getX(),button.getY(),button.getX()+cell-1,button.getY()+cell-1,0x77000000);
            if(button.isHovered()) g.setTooltipForNextFrame(font,item,mx,my);
        }
        if(!selected.isEmpty()){
            g.drawCenteredString(font,String.valueOf(quantity),side+sideWidth/2,y+146,ShopTheme.GOLD);
            g.drawString(font,font.plainSubstrByWidth(stack(selected).getHoverName().getString(),sideWidth),side,y+57,0xFFFFFFFF);
            for(int i=0;i<options.size();i++) if(options.get(i).isHovered()){
                int quantity=amounts[i]==0?available():amounts[i];
                g.setTooltipForNextFrame(text("total",Money.format(total(quantity))),mx,my);
            }
        }else g.drawString(font,font.plainSubstrByWidth(text("select").getString(),sideWidth),side,y+76,0xFFD2D9BF);
        if(customSell!=null&&customSell.isHovered())g.setTooltipForNextFrame(text("total",Money.format(total(quantity))),mx,my);
        String notice=Set.of("loading","select","special","ready","restricted","pending","timeout").contains(message)?text(message).getString():Component.translatable("shop.balancedshop."+message).getString();
        g.drawString(font,font.plainSubstrByWidth(notice,w-20),x+10,y+h-46,0xFFFFD590);
    }
    @Override public boolean isPauseScreen(){return false;}
}
