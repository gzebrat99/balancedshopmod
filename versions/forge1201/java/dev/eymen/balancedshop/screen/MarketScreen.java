package dev.eymen.balancedshop.screen;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.client.ShopClient;
import dev.eymen.balancedshop.menu.ShopMenu;
import dev.eymen.balancedshop.network.MarketNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import java.util.*;

/** Dark fantasy view. The server remains responsible for balances, stock and exact batch quotes. */
public final class MarketScreen extends AbstractContainerScreen<ShopMenu> {
    private static final String[] KEYS={"","@mine","@blocks","@farm","@gear","@rare"};
    private static final String[] NAMES={"Tüm eşyalar","Madenler","Bloklar","Tarım","Silah / Zırh","Özel / Nadir"};
    private static final String[] SHORT={"Tümü","Maden","Blok","Tarım","Ekipman","Nadir"};
    private static final Item[] ICONS={Items.CHEST,Items.DIAMOND,Items.STONE,Items.WHEAT,Items.IRON_SWORD,Items.NETHER_STAR};
    private MarketNetwork.Page data;
    private EditBox search;
    private String filter="",searchText="",selected="",status="";
    private int quantity=1,page,ticks,dirty=1,tradeCooldown;
    private int sidebar,gridX,gridY,gridWidth,gridHeight,columns,gridRows,capacity,cellWidth,cellHeight,rightX,rightWidth,previewSize,nameY,stockY,quantityY;
    private long sequence,lastAccepted=-1;
    private Button buy,sell,previous,next;
    private final List<Button> rows=new ArrayList<>(),tabs=new ArrayList<>();
    private final Map<String,ArrayDeque<Long>> history=new LinkedHashMap<>();
    public MarketScreen(ShopMenu menu,Inventory inventory,Component title){super(menu,inventory,title);filter=dev.eymen.balancedshop.keybind.MarketKeybind.takeCategory();}
    private Button button(int x,int y,int w,int h,String label,int accent,Button.OnPress action){return addRenderableWidget(new RuneButton(leftPos+x,topPos+y,w,h,Component.literal(label),accent,action));}
    @Override protected void init(){
        int oldCapacity=capacity;imageWidth=Math.min(760,width-12);imageHeight=Math.min(440,height-12);super.init();
        sidebar=imageWidth>=560?108:imageWidth>=400?82:28;rightWidth=imageWidth>=560?170:imageWidth>=400?122:110;
        gridX=sidebar+18;rightX=imageWidth-rightWidth-10;gridWidth=rightX-gridX-10;gridY=65;gridHeight=imageHeight-54-gridY;
        columns=Math.max(3,Math.min(6,gridWidth/44));gridRows=Math.max(3,Math.min(5,gridHeight/36));capacity=columns*gridRows;cellWidth=gridWidth/columns;cellHeight=gridHeight/gridRows;
        if(oldCapacity!=0&&oldCapacity!=capacity){page=0;data=null;}rows.clear();tabs.clear();
        search=new EditBox(font,leftPos+gridX+3,topPos+43,gridWidth-6,16,Component.literal("Eşya ara"));search.setMaxLength(64);search.setValue(searchText);search.setHint(Component.literal("Eşya ara..."));
        search.setResponder(value->{searchText=value;page=0;dirty=4;invalidate();});addRenderableWidget(search);
        int tabHeight=Math.min(35,(imageHeight-99)/6);
        for(int i=0;i<KEYS.length;i++){int index=i;Button tab=button(10,41+i*tabHeight,sidebar,tabHeight-3,NAMES[i],RpgTheme.GOLD,b->{filter=KEYS[index];page=0;selected="";dirty=1;invalidate();});tabs.add(tab);}
        for(int i=0;i<capacity;i++){int index=i;rows.add(addRenderableWidget(new SlotButton(leftPos+gridX+(i%columns)*cellWidth,topPos+gridY+(i/columns)*cellHeight,cellWidth-4,cellHeight-4,index,b->{if(data!=null&&index<data.rows().size()){selected=data.rows().get(index).id();dirty=1;invalidate();}})));}
        previewSize=imageHeight<280?24:40;nameY=52+previewSize+7;stockY=nameY+12;quantityY=stockY+12;
        button(rightX+7,quantityY,20,16,"−",RpgTheme.GOLD,b->{quantity=quantity==0?1:Math.max(1,quantity-1);dirty=1;invalidate();});
        button(rightX+rightWidth-27,quantityY,20,16,"+",RpgTheme.GOLD,b->{quantity=quantity==0?1:Math.min(2304,quantity+1);dirty=1;invalidate();});
        int[] amounts={1,16,64,0};int pw=(rightWidth-12)/4;
        for(int i=0;i<amounts.length;i++){int n=amounts[i];button(rightX+6+i*pw,quantityY+20,pw-2,16,n==0?"Tümü":n+"x",RpgTheme.GOLD,b->{quantity=n;dirty=1;invalidate();});}
        sell=button(rightX+7,quantityY+42,rightWidth-14,24,"SAT",0xB96D75,b->request(2));buy=button(rightX+7,quantityY+70,rightWidth-14,24,"SATIN AL",0x7AC8A0,b->request(1));
        previous=button(gridX,imageHeight-46,22,17,"<",RpgTheme.GOLD,b->{page=Math.max(0,page-1);dirty=1;invalidate();});next=button(gridX+gridWidth-26,imageHeight-46,22,17,">",RpgTheme.GOLD,b->{page++;dirty=1;invalidate();});
        button(imageWidth-27,8,17,17,"×",RpgTheme.GOLD,b->onClose());
        button(imageWidth-47,8,17,17,"?",RpgTheme.GOLD,b->dev.eymen.balancedshop.screen.GuideScreen.open());
        String[] extras={"Kitaplar","Üreteç","Yükselt","Akçe","Satış"};int ew=(imageWidth-20)/5;
        for(int i=0;i<extras.length;i++){int n=i;button(10+i*ew,imageHeight-22,ew-3,16,extras[i],0x555F77,b->{switch(n){case 0->ShopClient.openBooks();case 1->ShopClient.openSpawners();case 2->ShopClient.openUpgrade();case 3->ShopClient.openCoins(null);default->ShopClient.openSell();}});}
        dirty=1;updateButtons();
    }
    private void invalidate(){if(buy!=null){buy.active=false;sell.active=false;}}
    public void accept(MarketNetwork.Page incoming){
        if(incoming.container()!=menu.containerId||!incoming.session().equals(menu.session)||incoming.sequence()<sequence||incoming.sequence()<=lastAccepted)return;
        lastAccepted=incoming.sequence();data=incoming;page=incoming.page();
        for(var row:incoming.rows()){var points=history.computeIfAbsent(row.id(),id->new ArrayDeque<>());if(points.isEmpty())points.add(row.previous());if(points.peekLast()!=row.buy())points.add(row.buy());while(points.size()>16)points.removeFirst();}
        while(history.size()>256)history.remove(history.keySet().iterator().next());
        if(!incoming.message().isEmpty())status=message(incoming.message());
        // Search/pagination must not leave a hidden old item in the purchase panel.
        if(incoming.rows().isEmpty()){
            if(!selected.isEmpty()){selected="";dirty=1;}
        }else if(incoming.rows().stream().noneMatch(row->row.id().equals(selected))){
            selected=incoming.rows().get(0).id();dirty=1;
        }
        updateButtons();
    }
    private void updateButtons(){
        if(buy==null)return;boolean ready=data!=null&&dirty==0&&tradeCooldown==0&&data.selected().equals(selected)&&data.sequence()==sequence;
        buy.active=ready&&data.buy().quantity()>0;sell.active=ready&&data.sell().quantity()>0;previous.active=page>0;next.active=data!=null&&(page+1)*capacity<data.total();
        for(int i=0;i<rows.size();i++){rows.get(i).active=data!=null&&i<data.rows().size();rows.get(i).setMessage(Component.literal(data!=null&&i<data.rows().size()?stack(data.rows().get(i).id()).getHoverName().getString():"Boş slot"));}
    }
    private void request(int operation){if(minecraft==null||minecraft.player==null)return;if(operation!=0)tradeCooldown=6;MarketNetwork.CHANNEL.sendToServer(new MarketNetwork.Request(menu.containerId,menu.session,++sequence,operation,selected,quantity,searchText,filter,page,data==null?-1:data.revision(),capacity));invalidate();}
    @Override protected void containerTick(){super.containerTick();search.tick();ticks++;if(tradeCooldown>0&&--tradeCooldown==0)updateButtons();if(dirty>0){if(--dirty==0)request(0);}else if(ticks%20==0)request(0);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(search.isFocused()&&key!=256)return search.keyPressed(key,scan,modifiers)||search.canConsumeInput();return super.keyPressed(key,scan,modifiers);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        RpgTheme.frame(g,leftPos,topPos,imageWidth,imageHeight,RpgTheme.pulse(),true);RpgTheme.line(g,leftPos+9,topPos+32,imageWidth-18,1,0x625139);g.drawString(font,"DENGELİ PAZAR",leftPos+12,topPos+13,RpgTheme.GOLD,false);
        int bw=Math.min(152,imageWidth-150),bx=leftPos+imageWidth-55-bw;RpgTheme.outline(g,bx,topPos+7,bw,21,0x805D4C31);RpgTheme.diamond(g,bx+12,topPos+17,10,0x302619);RpgTheme.item(g,new ItemStack(CoinItems.entry(2).item()),bx+4,topPos+9,16);
        g.drawString(font,font.plainSubstrByWidth(data==null?"...":compact(data.balance())+" coin",bw-25),bx+24,topPos+14,RpgTheme.pulse(),false);
        if(imageWidth>540)for(int i=leftPos+135;i<bx-16;i+=18)RpgTheme.diamond(g,i,topPos+17,2,i%3==0?RpgTheme.CYAN:0x66583F);
        RpgTheme.frame(g,leftPos+rightX,topPos+39,rightWidth,imageHeight-66,0x514F64,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);List<Component> hover=null;
        for(int i=0;i<tabs.size();i++){Button tab=tabs.get(i);boolean active=filter.equals(KEYS[i]);if(active){RpgTheme.outline(g,tab.getX()-1,tab.getY()-1,tab.getWidth()+2,tab.getHeight()+2,0xFF000000|RpgTheme.GOLD);RpgTheme.line(g,tab.getX()+1,tab.getY()+2,2,tab.getHeight()-4,RpgTheme.GOLD);}RpgTheme.item(g,new ItemStack(ICONS[i]),tab.getX()+5,tab.getY()+(tab.getHeight()-16)/2,16);if(sidebar>35)g.drawString(font,SHORT[i],tab.getX()+25,tab.getY()+(tab.getHeight()-8)/2,active?RpgTheme.GOLD:RpgTheme.TEXT,false);if(tab.isHovered())hover=List.of(Component.literal(NAMES[i]).withStyle(s->s.withColor(RpgTheme.GOLD)));}
        if(data!=null){
            for(int i=0;i<Math.min(capacity,data.rows().size());i++){
                var row=data.rows().get(i);var button=rows.get(i);int size=Math.min(32,Math.min(button.getWidth()-10,button.getHeight()-16));
                RpgTheme.item(g,stack(row.id()),button.getX()+(button.getWidth()-size)/2,button.getY()+3,size);g.drawCenteredString(font,compact(row.buy()),button.getX()+button.getWidth()/2,button.getY()+button.getHeight()-10,RpgTheme.GOLD);
                int direction=Long.compare(row.buy(),row.previous());if(direction!=0)g.drawString(font,direction>0?"▲":"▼",button.getX()+button.getWidth()-8,button.getY()+3,direction>0?0x72E5A2:0xEF7888,false);
                if(button.isHovered())hover=priceTooltip(row);
            }
            if(data.total()==0)g.drawCenteredString(font,"Eşya bulunamadı",leftPos+gridX+gridWidth/2,topPos+gridY+16,RpgTheme.MUTED);
            g.drawCenteredString(font,(page+1)+" / "+Math.max(1,(data.total()+capacity-1)/capacity),leftPos+gridX+gridWidth/2,topPos+imageHeight-41,RpgTheme.MUTED);
        }
        int center=leftPos+rightX+rightWidth/2;
        if(!selected.isEmpty()){
            RpgTheme.frame(g,center-previewSize/2-3,topPos+49,previewSize+6,previewSize+6,0x665F84,false);RpgTheme.item(g,stack(selected),center-previewSize/2,topPos+52,previewSize);
            g.drawCenteredString(font,font.plainSubstrByWidth(stack(selected).getHoverName().getString(),rightWidth-14),center,topPos+nameY,RpgTheme.TEXT);
            var chosen=data==null?Optional.<MarketNetwork.Row>empty():data.rows().stream().filter(r->r.id().equals(selected)).findFirst();g.drawCenteredString(font,chosen.map(r->"Stok: "+r.stock()).orElse("Eşya seçili"),center,topPos+stockY,RpgTheme.MUTED);
            if(imageHeight>350&&chosen.isPresent())drawHistory(g,chosen.get(),leftPos+rightX+10,topPos+quantityY+111,rightWidth-20);
        }else g.drawCenteredString(font,"Bir eşya seç",center,topPos+nameY,RpgTheme.MUTED);
        g.drawCenteredString(font,quantity==0?"Tümü":quantity+"x",center,topPos+quantityY+4,RpgTheme.TEXT);
        if(data!=null&&data.selected().equals(selected)){
            drawOrder(g,sell,"SAT",data.sell(),0xDC959B);drawOrder(g,buy,"SATIN AL",data.buy(),0x9ADDB3);
            if(sell.isHovered()||buy.isHovered()){var q=buy.isHovered()?data.buy():data.sell();hover=List.of(Component.literal("Toplam: "+Money.format(q.total())+" coin").withStyle(s->s.withColor(RpgTheme.GOLD)),Component.literal("Miktar: "+q.quantity()),Component.literal("Temel: "+Money.format(data.base())),Component.literal("Alt / Üst: "+Money.format(data.min())+" / "+Money.format(data.max())),Component.literal("Her birim stok fiyatından hesaplanır."));}
        }
        if(imageHeight>300&&!status.isEmpty())g.drawCenteredString(font,font.plainSubstrByWidth(status,rightWidth-12),center,topPos+imageHeight-44,RpgTheme.GOLD);
        int bw=Math.min(152,imageWidth-150),bx=leftPos+imageWidth-55-bw;if(data!=null&&mx>=bx&&mx<bx+bw&&my>=topPos+7&&my<topPos+28)hover=List.of(Component.literal("BAKİYE").withStyle(s->s.withColor(RpgTheme.GOLD)),Component.literal(Money.format(data.balance())+" coin"));
        if(hover!=null)g.renderComponentTooltip(font,hover,mx,my);
    }
    private void drawOrder(GuiGraphics g,Button button,String label,dev.eymen.balancedshop.data.DynamicMarket.Quote q,int color){int center=button.getX()+button.getWidth()/2;g.drawCenteredString(font,label+" ("+(q.quantity()>0?q.quantity():quantity==0?"Tümü":quantity)+"x)",center,button.getY()+3,button.active?RpgTheme.TEXT:RpgTheme.MUTED);g.drawCenteredString(font,q.quantity()>0?compact(q.total())+" coin":message(q.message()),center,button.getY()+13,color);}
    public static List<Component> priceTooltip(MarketNetwork.Row row){
        long delta=row.buy()-row.previous();int color=delta>0?0x72E5A2:delta<0?0xEF7888:RpgTheme.MUTED;String trend=delta==0?"▬ Değişmedi":(delta>0?"▲ +":"▼ −")+String.format(Locale.ROOT,"%.2f%%",Math.abs(delta)*100.0/Math.max(1,row.previous()));
        return List.of(stack(row.id()).getHoverName().copy().withStyle(s->s.withColor(RpgTheme.GOLD)),Component.literal("Alış: "+Money.format(row.buy())+" coin"),Component.literal("Satış: "+Money.format(row.sell())+" coin"),Component.literal(trend+" · son işleme göre").withStyle(s->s.withColor(color)),Component.literal("Önceki alış: "+Money.format(row.previous())),Component.literal("Stok: "+row.stock()).withStyle(s->s.withColor(RpgTheme.CYAN)));
    }
    private void drawHistory(GuiGraphics g,MarketNetwork.Row row,int x,int y,int w){
        var points=history.get(row.id());g.drawString(font,"FİYAT HAREKETİ",x,y,RpgTheme.MUTED,false);RpgTheme.outline(g,x,y+12,w,34,0xFF343748);
        if(points==null||points.size()<2){g.drawString(font,"Henüz değişim yok",x+4,y+23,RpgTheme.MUTED,false);return;}
        long min=Collections.min(points),max=Collections.max(points);int i=0,px=x+3,py=y+29;
        for(long value:points){int nx=x+3+i*(w-7)/(points.size()-1),ny=y+41-(int)((value-min)*25/Math.max(1,max-min));if(i>0){int steps=Math.max(Math.abs(nx-px),Math.abs(ny-py));for(int t=0;t<=steps;t++)RpgTheme.line(g,px+(nx-px)*t/Math.max(1,steps),py+(ny-py)*t/Math.max(1,steps),1,1,row.buy()>=row.previous()?0x72E5A2:0xEF7888);}px=nx;py=ny;i++;}g.drawString(font,"Görülen son "+points.size()+" fiyat",x,y+50,RpgTheme.MUTED,false);
    }
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)));}
    private static String compact(long cents){if(cents>=100_000_000_000L)return String.format(Locale.ROOT,"%.1fB",cents/100_000_000_000.0);if(cents>=100_000_000)return String.format(Locale.ROOT,"%.1fM",cents/100_000_000.0);if(cents>=100_000)return String.format(Locale.ROOT,"%.1fk",cents/100_000.0);return Money.format(cents);}
    private static String message(String code){return switch(code){case "bought"->"Alış tamamlandı";case "sold"->"Satış tamamlandı";case "funds"->"Bakiye yok";case "items"->"Eşya yok";case "space"->"Yer yok";case "stock"->"Stok sınırı";case "changed"->"Fiyat yenilendi";case "cooldown"->"Biraz bekle";case "limit"->"Bakiye sınırı";case "restricted"->"Kapalı";default->"Eşya seç";};}
    private class RuneButton extends Button {
        private final int accent;
        RuneButton(int x,int y,int w,int h,Component text,int accent,OnPress action){super(x,y,w,h,text,action,DEFAULT_NARRATION);this.accent=accent;}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float partial){boolean hot=active&&isHoveredOrFocused();g.fillGradient(getX(),getY(),getX()+width,getY()+height,hot?0xF12C3040:0xE11A1C29,0xE1080A13);RpgTheme.outline(g,getX(),getY(),width,height,0xFF000000|(hot?accent:0x414455));RpgTheme.outline(g,getX()+2,getY()+2,width-4,height-4,(hot?0xA0000000:0x35000000)|accent);if(this!=buy&&this!=sell&&!tabs.contains(this))g.drawCenteredString(font,font.plainSubstrByWidth(getMessage().getString(),width-4),getX()+width/2,getY()+(height-8)/2,active?RpgTheme.TEXT:0x676A7B);}
    }
    private final class SlotButton extends Button {
        private final int index;
        SlotButton(int x,int y,int w,int h,int index,OnPress press){super(x,y,w,h,Component.empty(),press,DEFAULT_NARRATION);this.index=index;}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float partial){boolean picked=data!=null&&index<data.rows().size()&&data.rows().get(index).id().equals(selected);int accent=picked?RpgTheme.GOLD:isHoveredOrFocused()?RpgTheme.CYAN:0x4C526F;if(data!=null&&index<data.rows().size()&&!picked&&!isHoveredOrFocused())accent=switch(stack(data.rows().get(index).id()).getRarity()){case EPIC->0x9478C4;case RARE->0x638CC5;case UNCOMMON->0x708EAC;default->0x4C526F;};RpgTheme.frame(g,getX(),getY(),width,height,accent,picked||isHoveredOrFocused());}
    }
}
