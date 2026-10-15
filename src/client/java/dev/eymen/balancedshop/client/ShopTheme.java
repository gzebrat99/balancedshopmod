package dev.eymen.balancedshop.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** Animated farm market skin. Keeps vanilla keyboard narration. */
public final class ShopTheme {
    public static final int TEAL=0xFFAEDC86,GOLD=0xFFECC772,VIOLET=0xFFDAAC72;
    public static void card(GuiGraphics g,int x,int y,int w,int h){g.fillGradient(x,y,x+w,y+h,0xFF223421,0xFF17261B);g.renderOutline(x,y,w,h,0xFF536842);}
    public static double seconds(){return System.nanoTime()/1_000_000_000.0;}
    public static void gear(GuiGraphics g,int cx,int cy,int radius,int color){
        double phase=seconds()*.25;int faint=0x55000000|(color&0xFFFFFF);
        for(int i=0;i<48;i++){double a=i*Math.PI/24;int px=cx+(int)Math.round(Math.cos(a)*(radius-3)),py=cy+(int)Math.round(Math.sin(a)*(radius-3));g.fill(px,py,px+1,py+1,faint);}
        for(int i=0;i<12;i++){double a=phase+i*Math.PI/6;int px=cx+(int)Math.round(Math.cos(a)*radius),py=cy+(int)Math.round(Math.sin(a)*radius);g.fill(px-1,py-1,px+2,py+2,0x99000000|(color&0xFFFFFF));}
    }
    public static void panel(GuiGraphics g,int width,int height,int x,int y,int w,int h,int accent){
        g.fillGradient(0,0,width,height,0xEF0A120E,0xE31E3021);
        for(int i=0;i<5;i++) g.renderOutline(x-i-1,y-i-1,w+2*i+2,h+2*i+2,(14-i*2)<<24|(accent&0xFFFFFF));
        g.fillGradient(x,y,x+w,y+h,0xFF263727,0xFF142018);
        g.renderOutline(x,y,w,h,0xFF6B7850);
        g.fillGradient(x+1,y+1,x+w-1,y+38,0xFF39492D,0xFF243525);
        g.fill(x+1,y+1,x+w-1,y+3,accent);
        g.fill(x+8,y+h-54,x+w-8,y+h-53,0xFF52633D);
        g.fill(x+1,y+h-33,x+w-1,y+h-1,0xFF101A13);
        double time=seconds();int shimmer=(int)((time*42)%(w+64))-32;
        for(int i=0;i<30;i++){int px=x+shimmer+i;if(px>x&&px<x+w)g.fill(px,y+1,px+1,y+3,((int)(110*Math.sin(Math.PI*i/30))<<24)|(accent&0xFFFFFF));}
        for(int row=y+42;row<y+h-54;row+=24)g.fill(x+6,row,x+w-6,row+1,0x0DCCDB9D);
        // Slow lights live in the margin, leaving item names and controls stable.
        for(int i=0;i<8;i++){double t=time*.25+i*.83;int px=x-7-(i%2)*4,py=y+8+(int)((Math.sin(t)*.5+.5)*(h-16));g.fill(px,py,px+2,py+2,0x607D9D53);}
        int dots=(int)(time*1.8)%3;
        for(int i=0;i<3;i++)g.fill(x+w-9-i*4,y+h-4,x+w-7-i*4,y+h-3,i==dots?accent:0xFF4D6040);
    }
    public static Builder builder(Component label,Button.OnPress action){return new Builder(label,action);}
    public static final class Builder {
        private final Component label;private final Button.OnPress action;private int x,y,w,h;
        private Builder(Component label,Button.OnPress action){this.label=label;this.action=action;}
        public Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
        public Button build(){return button(label,action,x,y,w,h);}
    }
    public static Button button(Component label,Button.OnPress action,int x,int y,int w,int h){return new AuroraButton(x,y,w,h,label,action);}
    public static final class AuroraButton extends Button {
        private float glow;private long lastFrame;
        AuroraButton(int x,int y,int w,int h,Component label,OnPress action){super(x,y,w,h,label,action,DEFAULT_NARRATION);}
        @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean hover=isHoveredOrFocused();
            long now=System.nanoTime();float elapsed=lastFrame==0?0.016f:Math.min(.1f,(now-lastFrame)/1_000_000_000f);lastFrame=now;glow+=( (active&&hover?1:0)-glow)*Math.min(1,elapsed*12);
            int top=!active?0xFF253022:hover?0xFF526A37:0xFF34452B,bottom=!active?0xFF172016:hover?0xFF354B29:0xFF223322;
            g.fillGradient(x,y,x+w,y+h,top,bottom);g.renderOutline(x,y,w,h,active&&hover?TEAL:0xFF607448);
            if(active) g.fill(x+1,y+1,x+w-1,y+2,hover?TEAL:0xFF7F9159);
            if(glow>.02f)g.renderOutline(x-1,y-1,w+2,h+2,((int)(glow*90)<<24)|(TEAL&0xFFFFFF));
            if(!getMessage().getString().isEmpty()) g.drawCenteredString(Minecraft.getInstance().font,Minecraft.getInstance().font.plainSubstrByWidth(getMessage().getString(),w-6),x+w/2,y+(h-8)/2,active?0xFFF5EFD7:0xFF98A184);
        }
    }
}
