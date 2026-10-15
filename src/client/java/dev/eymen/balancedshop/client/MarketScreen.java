package dev.eymen.balancedshop.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** The custom panel owns its background; vanilla blur must run before UI drawing. */
public abstract class MarketScreen extends Screen {
    protected MarketScreen(Component title){super(title);}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){}
}
