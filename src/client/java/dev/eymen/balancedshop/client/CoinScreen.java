package dev.eymen.balancedshop.client;
import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class CoinScreen extends MarketScreen {
    private final Screen parent;
    private int x,y,w,h,rowH,serial,denomination=1,quantity=1,ticks;
    private boolean loading,waiting;
    private String message="loading";
    private CoinPackets.Page data;
    private final List<Button> rows=new ArrayList<>(),presets=new ArrayList<>();
    private Button minus,plus,withdraw,deposit,all;
    public CoinScreen(Screen parent){super(Component.translatable("coins.balancedshop.title"));this.parent=parent;}
    private Component text(String key,Object...args){return Component.translatable("coins.balancedshop."+key,args);}
    @Override protected void init(){
        w=Math.min(540,width-8);h=Math.min(350,height-8);x=(width-w)/2;y=(height-h)/2;rowH=Math.max(21,Math.min(37,(h-144)/4));
        addRenderableWidget(ShopTheme.button(Component.translatable("coins.balancedshop.shop"),b->ShopClient.open(),x+w-78,y+8,68,16));
        rows.clear();int rw=w-160;
        for(int i=0;i<4;i++){int index=i;rows.add(addRenderableWidget(ShopTheme.button(Component.empty(),b->{denomination=index;refresh();},x+10,y+52+i*rowH,rw,rowH-2)));}
        int sx=x+w-138;
        minus=addRenderableWidget(ShopTheme.button(Component.literal("<"),b->{quantity=Math.max(1,quantity-1);refresh();},sx,y+54,24,20));
        plus=addRenderableWidget(ShopTheme.button(Component.literal(">"),b->{quantity=Math.min(CoinEngine.MAX_QUANTITY,quantity+1);refresh();},sx+104,y+54,24,20));
        presets.clear();int[] counts={1,16,32,64};for(int i=0;i<4;i++){int count=counts[i];presets.add(addRenderableWidget(ShopTheme.button(Component.literal(""+count),b->{quantity=count;refresh();},sx+i*32,y+80,30,18)));}
        withdraw=addRenderableWidget(ShopTheme.button(text("withdraw"),b->action(0),sx,y+105,128,20));
        deposit=addRenderableWidget(ShopTheme.button(text("deposit"),b->action(1),sx,y+130,128,20));
        all=addRenderableWidget(ShopTheme.button(text("all"),b->action(2),x+10,y+h-80,w-20,20));
        addRenderableWidget(ShopTheme.button(text("back"),b->onClose(),x+w-106,y+h-28,96,20));
        request();
    }
    private void request(){serial=ShopClient.nextSerial();loading=true;waiting=false;ticks=0;refresh();ClientPlayNetworking.send(new CoinPackets.Query(serial));}
    public void accept(CoinPackets.Page p){if(p.serial()!=serial)return;data=p;loading=waiting=false;ticks=0;message=p.message().isEmpty()?"hint":p.message();refresh();}
    private void refresh(){if(withdraw==null)return;boolean ready=data!=null&&!loading&&!waiting;long cost=CoinItems.entry(denomination).value()*quantity;
        rows.forEach(b->b.active=ready);presets.forEach(b->b.active=ready);minus.active=ready&&quantity>1;plus.active=ready&&quantity<CoinEngine.MAX_QUANTITY;
        withdraw.active=ready&&data.balance()>=cost;deposit.active=ready&&data.counts().get(denomination)>=quantity;all.active=ready&&data.physical()>0;
    }
    private void action(int operation){if(data==null||loading||waiting)return;waiting=true;ticks=0;message="pending";refresh();ClientPlayNetworking.send(new CoinPackets.Action(serial,data.token(),operation,denomination,quantity));}
    @Override public void tick(){if((loading||waiting)&&++ticks>100){message="timeout";request();}if(minecraft.player==null)onClose();}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ShopTheme.panel(g,width,height,x,y,w,h,ShopTheme.GOLD);g.drawString(font,title,x+10,y+10,ShopTheme.GOLD);
        if(data!=null){g.drawString(font,font.plainSubstrByWidth(text("bank",Money.format(data.balance())).getString(),w-20),x+10,y+26,ShopTheme.TEAL);g.drawString(font,font.plainSubstrByWidth(text("physical",Money.format(data.physical())).getString(),w-20),x+10,y+40,0xFFAABBD4);}
        super.render(g,mx,my,delta);
        for(int i=0;i<4;i++){var row=rows.get(i);var coin=CoinItems.entry(i);int ry=row.getY();if(i==denomination)g.renderOutline(row.getX(),ry,row.getWidth(),row.getHeight(),ShopTheme.GOLD);
            g.renderFakeItem(new ItemStack(coin.item()),row.getX()+4,ry+(rowH-16)/2+(int)(Math.sin(ShopTheme.seconds()*2+i)*1));
            String label=new ItemStack(coin.item()).getHoverName().getString();g.drawString(font,font.plainSubstrByWidth(label,row.getWidth()-30),row.getX()+26,ry+3,0xFFF2E5BF);
            if(rowH>=28)g.drawString(font,font.plainSubstrByWidth(Money.format(coin.value())+" coin · "+(data==null?0:data.counts().get(i))+"x",row.getWidth()-30),row.getX()+26,ry+16,0xFFAABBD4);
            if(row.isHovered())g.setTooltipForNextFrame(font,List.of(Component.literal(label),text("value",Money.format(coin.value())),text("owned",data==null?0:data.counts().get(i))),Optional.empty(),mx,my);
        }
        int sx=x+w-138;g.drawCenteredString(font,Component.literal(""+quantity),sx+64,y+60,ShopTheme.GOLD);
        if(h>=280)g.drawString(font,font.plainSubstrByWidth(Money.format(CoinItems.entry(denomination).value()*quantity)+" coin",128),sx,y+159,ShopTheme.TEAL);
        Component notice=Set.of("hint","withdrawn","deposited").contains(message)?text(message):Component.translatable("shop.balancedshop."+message);
        g.drawString(font,font.plainSubstrByWidth(notice.getString(),w-20),x+10,y+h-46,ShopTheme.GOLD);
        g.drawString(font,text("denominations"),x+10,y+h-22,0xFFAABBD4);
    }
    @Override public void onClose(){minecraft.setScreen(parent==null?new ShopScreen():parent);}
    @Override public boolean isPauseScreen(){return false;}
}
