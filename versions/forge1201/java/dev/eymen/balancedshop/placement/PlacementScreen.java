package dev.eymen.balancedshop.placement;

import dev.eymen.balancedshop.screen.RpgTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PlacementScreen extends Screen {
    private int x,y,w,h;
    public PlacementScreen(){super(Component.literal("Alan Yerleştirme"));}
    @Override protected void init(){
        w=Math.min(340,width-12);h=Math.min(220,height-12);x=(width-w)/2;y=(height-h)/2;
        for(int value=3;value<=10;value++){
            final int n=value;int i=value-3;
            addRenderableWidget(Button.builder(Component.literal(n+"×"+n),b->{PlacementClient.select(n);}).bounds(x+14+(i%4)*(w-28)/4,y+48+(i/4)*26,(w-28)/4-4,22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Tekli yerleştir"),b->{PlacementClient.disable();onClose();}).bounds(x+14,y+h-30,(w-32)/2,20).build());
        addRenderableWidget(Button.builder(Component.literal("Tamam"),b->onClose()).bounds(x+18+(w-32)/2,y+h-30,(w-32)/2,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g);RpgTheme.frame(g,x,y,w,h,RpgTheme.GOLD,true);
        g.drawCenteredString(font,title,x+w/2,y+14,RpgTheme.GOLD);
        g.drawCenteredString(font,"Seçili: "+PlacementClient.size()+"×"+PlacementClient.size()+" · "+(PlacementClient.size()*PlacementClient.size())+" blok",x+w/2,y+31,RpgTheme.TEXT);
        g.drawCenteredString(font,PlacementClient.enabled()?"Alan açık · Sağ tık ile yerleştir":"Alan kapalı · Açmak için boyut seç",x+w/2,y+112,RpgTheme.GOLD);
        g.drawCenteredString(font,"Ana elindeki blokları envanterinden kullanır.",x+w/2,y+128,RpgTheme.MUTED);
        g.drawCenteredString(font,"Önizleme: altın çizgiler · dolu hücreler kırmızı",x+w/2,y+142,RpgTheme.MUTED);
        super.render(g,mx,my,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
}
