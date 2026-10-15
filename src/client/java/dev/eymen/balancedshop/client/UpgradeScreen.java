package dev.eymen.balancedshop.client;
import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class UpgradeScreen extends MarketScreen {
    private int x,y,w,h,cell,leftWidth,right,rightWidth,rowHeight,slot,page,serial,debounce=-1,ticks,animation;
    private boolean enchant,loading,waiting;
    private double rolledChance;private long rolledFee;private ItemStack rolledSource=ItemStack.EMPTY;
    private int give=1,receive=1,rolledGive=1;
    private String query="",selected="",message="hint";
    private UpgradePackets.Page data;
    private EditBox search;
    private Button roll,previous,next,mode;
    private Button giveMinus,givePlus,receiveMinus,receivePlus,giveLabel,receiveLabel;
    private final List<Button> givePresets=new ArrayList<>(),receivePresets=new ArrayList<>();
    private static final int[] PRESETS={16,32,64,256};
    private final List<Button> slots=new ArrayList<>(),rows=new ArrayList<>();
    public UpgradeScreen(){super(Component.translatable("upgrade.balancedshop.title"));}
    private Component text(String key,Object...args){return Component.translatable("upgrade.balancedshop."+key,args);}
    @Override protected void init(){
        w=Math.min(620,width-8);h=Math.min(390,height-8);x=(width-w)/2;y=(height-h)/2;
        leftWidth=(w-30)/2;cell=Math.max(16,Math.min(29,Math.min(leftWidth/9,(h-168)/4)));right=x+20+leftWidth;rightWidth=w-leftWidth-30;rowHeight=Math.max(16,Math.min(31,(h-168)/UpgradePackets.PAGE_SIZE));
        mode=addRenderableWidget(ShopTheme.builder(text(enchant?"enchant":"exchange"),b->{enchant=!enchant;selected="";give=receive=1;page=0;query="";search.setValue("");request();}).bounds(x+10,y+42,leftWidth,20).build());
        search=addRenderableWidget(new EditBox(font,right,y+42,rightWidth,20,text("search")));search.setMaxLength(64);search.setValue(query);search.setHint(text("search"));search.setResponder(v->{query=v;page=0;selected="";debounce=6;});
        slots.clear();rows.clear();givePresets.clear();receivePresets.clear();
        giveMinus=addRenderableWidget(ShopTheme.builder(Component.literal("<"),b->amount(true,-1)).bounds(x+10,y+66,20,18).build());
        givePlus=addRenderableWidget(ShopTheme.builder(Component.literal(">"),b->amount(true,1)).bounds(x+10+leftWidth-20,y+66,20,18).build());
        giveLabel=addRenderableWidget(ShopTheme.builder(Component.empty(),b->{}).bounds(x+32,y+66,leftWidth-44,18).build());giveLabel.active=false;
        receiveMinus=addRenderableWidget(ShopTheme.builder(Component.literal("<"),b->amount(false,-1)).bounds(right,y+66,20,18).build());
        receivePlus=addRenderableWidget(ShopTheme.builder(Component.literal(">"),b->amount(false,1)).bounds(right+rightWidth-20,y+66,20,18).build());
        receiveLabel=addRenderableWidget(ShopTheme.builder(Component.empty(),b->{}).bounds(right+22,y+66,rightWidth-44,18).build());receiveLabel.active=false;
        for(int i=0;i<PRESETS.length;i++){
            int count=PRESETS[i],gw=leftWidth/4,rw=(rightWidth-44)/4;
            givePresets.add(addRenderableWidget(ShopTheme.builder(Component.literal(String.valueOf(count)),b->{give=count;request();}).bounds(x+10+i*gw,y+86,gw-2,16).build()));
            receivePresets.add(addRenderableWidget(ShopTheme.builder(Component.literal(String.valueOf(count)),b->{receive=count;request();}).bounds(right+22+i*rw,y+86,rw-2,16).build()));
        }
        for(int i=0;i<36;i++){
            int inventorySlot=i<27?i+9:i-27;
            slots.add(addRenderableWidget(ShopTheme.builder(Component.empty(),b->{slot=inventorySlot;selected="";give=receive=1;page=0;request();}).bounds(x+10+i%9*cell,y+106+i/9*cell+(i>=27?4:0),cell-1,cell-1).build()));
        }
        for(int i=0;i<UpgradePackets.PAGE_SIZE;i++){
            int index=i;rows.add(addRenderableWidget(ShopTheme.builder(Component.empty(),b->{if(data!=null&&index<data.targets().size()){selected=data.targets().get(index).key();request();}}).bounds(right,y+106+i*rowHeight,rightWidth,rowHeight-1).build()));
        }
        previous=addRenderableWidget(ShopTheme.builder(Component.literal("<"),b->{page--;selected="";request();}).bounds(right,y+86,20,16).build());
        next=addRenderableWidget(ShopTheme.builder(Component.literal(">"),b->{page++;selected="";request();}).bounds(right+rightWidth-20,y+86,20,16).build());
        roll=addRenderableWidget(ShopTheme.builder(text("roll"),b->roll()).bounds(x+w-140,y+h-24,130,18).build());
        addRenderableWidget(ShopTheme.builder(Component.translatable("books.balancedshop.back"),b->ShopClient.open()).bounds(x+10,y+h-24,80,18).build());
        addRenderableWidget(ShopTheme.button(Component.translatable("coins.balancedshop.open"),b->ShopClient.openCoins(this),x+w-78,y+25,68,12));
        refresh();request();
    }
    private void amount(boolean source,int difference){if(source) give=Math.max(1,Math.min(UpgradeEngine.MAX_QUANTITY,give+difference));else receive=Math.max(1,Math.min(UpgradeEngine.MAX_QUANTITY,receive+difference));request();}
    private void request(){debounce=-1;serial=ShopClient.nextSerial();loading=true;ticks=0;message="hint";refresh();ClientPlayNetworking.send(new UpgradePackets.Query(serial,slot,enchant,Math.max(0,page),query,selected,give,receive));}
    public void accept(UpgradePackets.Page p){if(p.serial()!=serial) return;data=p;slot=p.slot();page=p.page();selected=p.selected();give=p.give();receive=p.receive();loading=false;waiting=false;ticks=0;if(!p.message().isEmpty()) message=p.message();refresh();}
    private void refresh(){
        if(roll==null) return;boolean ready=data!=null&&!waiting&&!loading&&animation==0;
        mode.setMessage(text(enchant?"enchant":"exchange"));
        giveLabel.setMessage(text("give",give,data==null?0:data.available()));receiveLabel.setMessage(text("receive",receive));
        giveMinus.active=ready&&give>1;givePlus.active=ready&&give<Math.min(UpgradeEngine.MAX_QUANTITY,data.available());
        receiveMinus.active=ready&&receive>1;receivePlus.active=ready&&receive<UpgradeEngine.MAX_QUANTITY;
        for(int i=0;i<PRESETS.length;i++){givePresets.get(i).active=ready&&PRESETS[i]<=data.available();receivePresets.get(i).active=ready;}
        roll.active=ready&&!selected.isEmpty()&&data.chance()>0&&data.balance()>=data.fee();mode.active=ready;search.active=ready;
        for(var b:slots) b.active=ready;for(int i=0;i<rows.size();i++){rows.get(i).visible=data!=null&&i<data.targets().size();rows.get(i).active=ready;}
        previous.active=ready&&page>0;next.active=ready&&(page+1)*UpgradePackets.PAGE_SIZE<data.total();
    }
    private void roll(){if(!roll.active) return;rolledGive=give;rolledChance=data.chance();rolledFee=data.fee();rolledSource=data.inventory().get(slot).copy();waiting=true;ticks=0;animation=36;message="rolling";refresh();ClientPlayNetworking.send(new UpgradePackets.Roll(serial,data.token()));}
    @Override public void tick(){
        if(animation>0&&--animation==0) refresh();
        if(debounce>=0&&--debounce==0){debounce=-1;request();}
        if((loading||waiting)&&++ticks>100){waiting=false;message="timeout";request();}
        if(minecraft.player==null) onClose();
    }
    private Component targetName(UpgradeEngine.Target t){
        int at=t.key().lastIndexOf('@');if(at<0) return t.result().getHoverName();
        String id=t.key().substring(0,at);int level=Integer.parseInt(t.key().substring(at+1));
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(t.result()).keySet().stream()
            .filter(holder->holder.unwrapKey().orElseThrow().identifier().toString().equals(id)).findFirst()
            .map(holder->net.minecraft.world.item.enchantment.Enchantment.getFullname(holder,level)).orElse(t.result().getHoverName());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ShopTheme.panel(g,width,height,x,y,w,h,ShopTheme.VIOLET);
        g.drawString(font,title,x+10,y+10,0xFFF1E9FF);
        g.drawString(font,text("subtitle"),x+w-font.width(text("subtitle"))-10,y+10,ShopTheme.GOLD);
        if(data!=null) g.drawString(font,Component.translatable("shop.balancedshop.balance",Money.format(data.balance())),x+10,y+26,ShopTheme.TEAL);
        if(data!=null) g.drawString(font,text("count",page+1,Math.max(1,(data.total()+UpgradePackets.PAGE_SIZE-1)/UpgradePackets.PAGE_SIZE),data.total()),right,y+26,0xFF9EADCA);
        ShopTheme.card(g,x+8,y+40,leftWidth+4,h-102);ShopTheme.card(g,right-2,y+40,rightWidth+4,h-102);
        super.render(g,mx,my,delta);
        if(data!=null){
            for(int i=0;i<36;i++){
                int actual=i<27?i+9:i-27;var stack=data.inventory().get(actual);var b=slots.get(i);int ix=b.getX()+(cell-16)/2,iy=b.getY()+(cell-16)/2;
                g.renderFakeItem(stack,ix,iy);g.renderItemDecorations(font,stack,ix,iy);
                if(actual==slot) g.renderOutline(b.getX(),b.getY(),cell-1,cell-1,ShopTheme.GOLD);
                if(!stack.isEmpty()&&!UpgradeEngine.eligible(stack,enchant)) g.fill(b.getX(),b.getY(),b.getX()+cell-1,b.getY()+cell-1,0x88000000);
                if(b.isHovered()&&!stack.isEmpty()) g.setTooltipForNextFrame(font,stack, mx,my);
            }
            for(int i=0;i<data.targets().size();i++){
                var target=data.targets().get(i);var b=rows.get(i);int bx=b.getX(),by=b.getY();g.renderFakeItem(target.result(),bx+2,by+(rowHeight-16)/2);
                if(target.key().equals(selected)) g.renderOutline(bx,by,rightWidth,rowHeight-1,ShopTheme.VIOLET);
                g.drawString(font,font.plainSubstrByWidth(targetName(target).getString(),rightWidth-24),bx+22,by+(rowHeight>=26?3:4),0xFFFFFFFF);
                if(rowHeight>=26) g.drawString(font,receive+"x · "+Money.format(target.value()*receive)+" coin",bx+22,by+15,ShopTheme.GOLD);
                if(b.isHovered()) g.setTooltipForNextFrame(font,List.of(targetName(target),Component.literal(target.key()),text("targetvalue",Money.format(target.value()*receive))),Optional.empty(),mx,my);
            }
            boolean outcome=animation>0||message.equals("success")||message.equals("failed");double shownChance=outcome?rolledChance:data.chance();
            String chance=String.format(Locale.ROOT,"%.2f%%",shownChance*100);
            g.drawString(font,font.plainSubstrByWidth(text("chance",chance).getString(),leftWidth),x+10,y+h-48,ShopTheme.VIOLET);
            g.fill(x+10,y+h-58,x+10+leftWidth,y+h-55,0xFF39482B);
            g.fill(x+10,y+h-58,x+10+(int)(leftWidth*shownChance),y+h-55,ShopTheme.TEAL);
            if(animation>0){int marker=x+10+(int)((Math.sin((36-animation+delta)*.65)*.5+.5)*(leftWidth-2));g.fill(marker,y+h-59,marker+2,y+h-54,ShopTheme.GOLD);}
            g.drawString(font,font.plainSubstrByWidth(text("fee",Money.format(outcome?rolledFee:data.fee())).getString(),rightWidth),right,y+h-48,ShopTheme.GOLD);
            g.drawString(font,font.plainSubstrByWidth(text(data.destroy()?"risk":"safe",outcome?rolledGive:give).getString(),w-20),x+10,y+h-36,0xFFFF9E9E);
            if(h>=300){
                int cx=x+10+leftWidth/2,cy=y+h-112,r=24;
                g.drawString(font,font.plainSubstrByWidth(text("sourcevalue",Money.format(data.value())).getString(),leftWidth),x+10,y+h-151,0xFF9EADCA);
                var chosen=data.targets().stream().filter(t->t.key().equals(selected)).findFirst().orElse(null);
                if(chosen!=null){
                    g.fillGradient(right,y+h-127,right+rightWidth,y+h-64,0xFF34482A,0xFF1C2D1D);g.renderOutline(right,y+h-127,rightWidth,63,0xFF7D8450);
                    g.renderFakeItem(chosen.result(),right+8,y+h-117);
                    g.drawString(font,font.plainSubstrByWidth(targetName(chosen).getString(),rightWidth-32),right+30,y+h-115,ShopTheme.VIOLET);
                    g.drawString(font,font.plainSubstrByWidth(text("targetvalue",Money.format(chosen.value()*receive)).getString(),rightWidth-16),right+8,y+h-93,ShopTheme.GOLD);
                    g.drawString(font,font.plainSubstrByWidth(text("batch",give,receive).getString(),rightWidth-16),right+8,y+h-78,0xFF9EADCA);
                }
                for(int i=0;i<72;i++){double a=i*Math.PI*2/72;int px=cx+(int)(Math.sin(a)*r),py=cy-(int)(Math.cos(a)*r);g.fill(px,py,px+2,py+2,i<72*shownChance?ShopTheme.TEAL:0xFF4B5D3A);}
                double phase=animation>0?(36-animation+delta)*.64:message.equals("success")?Math.PI*shownChance:message.equals("failed")?Math.PI*(1+shownChance):0;int px=cx+(int)(Math.sin(phase)*r),py=cy-(int)(Math.cos(phase)*r);g.fill(px-2,py-2,px+3,py+3,ShopTheme.GOLD);
                g.renderFakeItem(outcome?rolledSource:data.inventory().get(slot),cx-8,cy-8);
            }
        }
        String notice=animation>0?text("rolling").getString():Set.of("hint","success","failed","rolling").contains(message)?text(message).getString():Component.translatable("shop.balancedshop."+message).getString();
        g.drawCenteredString(font,font.plainSubstrByWidth(notice,w-240),x+95+(w-240)/2,y+h-19,message.equals("success")?ShopTheme.TEAL:ShopTheme.GOLD);
    }
    @Override public boolean isPauseScreen(){return false;}
}
