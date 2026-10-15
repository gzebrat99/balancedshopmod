package dev.eymen.balancedshop.keybind;

import dev.eymen.balancedshop.menu.ShopMenu;
import dev.eymen.balancedshop.network.MarketNetwork;
import dev.eymen.balancedshop.screen.MarketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Existing ShopClient B mapping and /shop both call this adapter; no duplicate B registration. */
public final class MarketKeybind {
    private static String initialCategory="";
    private MarketKeybind(){}
    public static void install(IEventBus bus){
        bus.addListener((FMLClientSetupEvent event)->event.enqueueWork(()->MenuScreens.register(ShopMenu.TYPE.get(),MarketScreen::new)));
        MarketNetwork.receiver=page->{if(Minecraft.getInstance().screen instanceof MarketScreen screen)screen.accept(page);};
    }
    public static String takeCategory(){String value=initialCategory;initialCategory="";return value;}
    public static void open(){open("");}
    public static void open(String category){var c=Minecraft.getInstance();if(c.player!=null){initialCategory=category;closeMarket();MarketNetwork.CHANNEL.sendToServer(new MarketNetwork.Open());}}
    /** Close the real container before entering the existing books/coins/upgrade screens. */
    public static void closeMarket(){var p=Minecraft.getInstance().player;if(p!=null&&p.containerMenu instanceof ShopMenu)p.closeContainer();}
}
