package dev.eymen.balancedshop.screen;

import dev.eymen.balancedshop.localization.TurkishText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/** The storage retains the vanilla chest protocol; only this mod's named screen is restyled. */
public final class SpawnerStorageScreen extends ContainerScreen {
    public SpawnerStorageScreen(ChestMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    public static void install(){
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Opening event)->{
            var client=Minecraft.getInstance();
            if(client.player!=null&&event.getNewScreen() instanceof ContainerScreen screen&&!(screen instanceof SpawnerStorageScreen)
                &&screen.getTitle().getString().equals("Üreteç Deposu · 4 saniye"))
                event.setNewScreen(new SpawnerStorageScreen(screen.getMenu(),client.player.getInventory(),screen.getTitle()));
        });
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        RpgTheme.frame(g,leftPos,topPos,imageWidth,imageHeight,RpgTheme.GOLD,true);
        for(var slot:menu.slots){
            int x=leftPos+slot.x-1,y=topPos+slot.y-1;
            g.fill(x,y,x+18,y+18,0xFF0A0D15);RpgTheme.outline(g,x,y,18,18,0xFF51586B);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,titleLabelX,titleLabelY,RpgTheme.GOLD,false);
        g.drawString(font,"Envanter",inventoryLabelX,inventoryLabelY,RpgTheme.TEXT,false);
    }
    @Override protected void renderTooltip(GuiGraphics g,int mouseX,int mouseY){
        if(menu.getCarried().isEmpty()&&hoveredSlot!=null&&hoveredSlot.hasItem()){
            var flag=minecraft.options.advancedItemTooltips?TooltipFlag.Default.ADVANCED:TooltipFlag.Default.NORMAL;
            var lines=hoveredSlot.getItem().getTooltipLines(minecraft.player,flag).stream().map(TurkishText::component).toList();
            g.renderComponentTooltip(font,lines,mouseX,mouseY);
        }
    }
}
