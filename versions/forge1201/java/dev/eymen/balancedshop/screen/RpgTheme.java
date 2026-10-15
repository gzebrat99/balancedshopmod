package dev.eymen.balancedshop.screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
/** Code-drawn metal, gilding and runes remain crisp at every GUI scale. */
public final class RpgTheme {
    public static final int GOLD=0xD9B876,TEXT=0xE9DFCA,MUTED=0x969AAE,CYAN=0x75C8D4;
    private RpgTheme(){}
    public static void line(GuiGraphics g,int x,int y,int w,int h,int rgb){g.fill(x,y,x+w,y+h,0xFF000000|rgb);}
    public static void outline(GuiGraphics g,int x,int y,int w,int h,int color){g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color);g.fill(x,y,x+1,y+h,color);g.fill(x+w-1,y,x+w,y+h,color);}
    public static void frame(GuiGraphics g,int x,int y,int w,int h,int accent,boolean glow){
        g.fill(x+2,y+3,x+w+2,y+h+3,0x70000000);
        if(glow){outline(g,x-2,y-2,w+4,h+4,0x18000000|accent);outline(g,x-1,y-1,w+2,h+2,0x40000000|accent);}
        g.fillGradient(x,y,x+w,y+h,0xE9181C29,0xE5080A11);outline(g,x,y,w,h,0xFF3B4051);outline(g,x+2,y+2,w-4,h-4,0xFF000000|accent);line(g,x+4,y+4,w-8,1,0x303343);
        for(int dx:new int[]{0,w-8})for(int dy:new int[]{0,h-8}){int cx=x+dx,cy=y+dy;g.fill(cx,cy,cx+8,cy+8,0xFF121620);line(g,cx+1,cy+1,6,1,0x788294);line(g,cx+1,cy+1,1,6,0x788294);diamond(g,cx+4,cy+4,2,accent);}
    }
    public static void diamond(GuiGraphics g,int x,int y,int radius,int rgb){for(int n=-radius;n<=radius;n++){int half=radius-Math.abs(n);g.fill(x-half,y+n,x+half+1,y+n+1,0xFF000000|rgb);}}
    public static void item(GuiGraphics g,ItemStack stack,int x,int y,int size){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/16f,size/16f,1);g.renderFakeItem(stack,0,0);g.pose().popPose();}
    public static int pulse(){double n=(Math.sin(System.currentTimeMillis()/750.0)+1)*0.5;return (int)(180+50*n)<<16|(int)(144+45*n)<<8|(int)(83+34*n);}
}
