package dev.eymen.balancedshop.keybind;

import dev.eymen.balancedshop.menu.ShopMenu;
import dev.eymen.balancedshop.network.MarketNetwork;
import dev.eymen.balancedshop.screen.MarketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

/** B and /shop share the same server-owned extended container. */
public final class MarketKeybind {
    private static String initialCategory="";
    public static void install(){
        MenuScreens.register(ShopMenu.TYPE.get(),MarketScreen::new);
        MarketNetwork.receiver=page->{if(Minecraft.getInstance().screen instanceof MarketScreen screen)screen.accept(page);};
    }
    public static String takeCategory(){String value=initialCategory;initialCategory="";return value;}
    public static void open(){open("");}
    public static void open(String category){var client=Minecraft.getInstance();if(client.player!=null){initialCategory=category;closeMarket();MarketNetwork.CHANNEL.sendToServer(new MarketNetwork.Open());}}
    public static void closeMarket(){var player=Minecraft.getInstance().player;if(player!=null&&player.containerMenu instanceof ShopMenu)player.closeContainer();}
}
