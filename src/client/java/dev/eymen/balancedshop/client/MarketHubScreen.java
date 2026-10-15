package dev.eymen.balancedshop.client;
import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
public final class MarketHubScreen extends MarketScreen {
    private int x,y,w,h,serial;
    private ShopPackets.Page data;
    private final String[] names={"farm","market","sell","books","upgrade","spawners","coins","create"};
    private final ItemStack[] icons={new ItemStack(Items.WHEAT),new ItemStack(Items.CHEST),new ItemStack(Items.EMERALD),new ItemStack(Items.ENCHANTED_BOOK),new ItemStack(Items.DIAMOND_PICKAXE),new ItemStack(Items.SPAWNER),new ItemStack(CoinItems.entry(2).item()),new ItemStack(Items.GRINDSTONE)};
    public MarketHubScreen(){super(Component.translatable("market.balancedshop.title"));}
    @Override protected void init(){
        w=Math.min(500,width-8);h=Math.min(310,height-8);x=(width-w)/2;y=(height-h)/2;
        int cw=(w-30)/4,ch=Math.max(35,(h-126)/2);
        for(int i=0;i<8;i++){
            final int index=i;addRenderableWidget(ShopTheme.button(Component.translatable("market.balancedshop."+names[i]),b->navigate(index),x+12+(i%4)*(cw+2),y+66+(i/4)*(ch+4),cw,ch));
        }
        addRenderableWidget(ShopTheme.button(Component.translatable("gui.done"),b->onClose(),x+w-92,y+h-28,80,20));
        serial=ShopClient.nextSerial();ClientPlayNetworking.send(new ShopPackets.Query(serial,0,"","@farm"));
    }
    private void navigate(int i){switch(i){case 0->ShopClient.openShop("@farm");case 1->ShopClient.openShop("");case 2->ShopClient.openSell();case 3->ShopClient.openBooks();case 4->ShopClient.openUpgrade();case 5->ShopClient.openSpawners();case 6->ShopClient.openCoins(this);default->ShopClient.openShop("create");}}
    public void accept(ShopPackets.Page page){if(page.serial()==serial)data=page;}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ShopTheme.panel(g,width,height,x,y,w,h,ShopTheme.GOLD);
        g.drawString(font,title,x+14,y+12,ShopTheme.GOLD);
        g.drawString(font,Component.translatable("market.balancedshop.subtitle"),x+14,y+30,0xFFD8D8BF);
        if(data!=null)g.drawString(font,Component.translatable("shop.balancedshop.balance",Money.format(data.balance())),x+14,y+48,ShopTheme.TEAL);
        super.render(g,mx,my,delta);
        int cw=(w-30)/4,ch=Math.max(35,(h-126)/2);
        for(int i=0;i<8;i++)g.renderFakeItem(icons[i],x+16+(i%4)*(cw+2),y+69+(i/4)*(ch+4));
        g.drawString(font,font.plainSubstrByWidth(Component.translatable("market.balancedshop.footer").getString(),w-24),x+12,y+h-47,0xFFC8D7B7);
    }
    @Override public boolean isPauseScreen(){return false;}
}
