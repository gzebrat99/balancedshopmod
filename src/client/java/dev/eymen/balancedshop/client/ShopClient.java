package dev.eymen.balancedshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.eymen.balancedshop.ShopPackets;
import dev.eymen.balancedshop.SpawnerPackets;
import dev.eymen.balancedshop.VeinPackets;
import dev.eymen.balancedshop.SellPackets;
import dev.eymen.balancedshop.BookPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ShopClient implements ClientModInitializer {
    private static int serial;
    public static int nextSerial() { return ++serial; }
    @Override public void onInitializeClient() {
        SellTooltip.register();
        ClientPlayNetworking.registerGlobalReceiver(dev.eymen.balancedshop.CoinPackets.Page.TYPE,(p,c)->{if(c.client().screen instanceof CoinScreen s)s.accept(p);});
        ClientPlayNetworking.registerGlobalReceiver(dev.eymen.balancedshop.TraderPackets.Open.TYPE,(p,c)->c.client().setScreen(new CreateTraderScreen()));
        ClientPlayNetworking.registerGlobalReceiver(dev.eymen.balancedshop.TraderPackets.Page.TYPE,(p,c)->{if(c.client().screen instanceof CreateTraderScreen s)s.accept(p);});
        ClientCommandRegistrationCallback.EVENT.register((d,a)->d.register(ClientCommandManager.literal("coins").executes(c->{openCoins(null);return 1;})));
        ClientPlayNetworking.registerGlobalReceiver(dev.eymen.balancedshop.UpgradePackets.Page.TYPE,(page,context)->{if(context.client().screen instanceof UpgradeScreen screen) screen.accept(page);});
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("upgrade").executes(context->{openUpgrade();return 1;})));
        KeyMapping key=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.balancedshop.open",InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,KeyMapping.Category.register(Identifier.fromNamespaceAndPath("balancedshop","shop"))));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.consumeClick()) if(client.screen==null) open();});
        KeyMapping coins=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.balancedshop.coins",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,key.getCategory()));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(coins.consumeClick())if(client.screen==null)openCoins(null);});
        KeyMapping upgrade=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.balancedshop.upgrade",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,key.getCategory()));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(upgrade.consumeClick()) if(client.screen==null) openUpgrade();});
        KeyMapping vein=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.balancedshop.vein",InputConstants.Type.MOUSE,3,key.getCategory()));
        final boolean[] held={false};final int[] heartbeat={0};
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            boolean down=client.player!=null&&client.screen==null&&vein.isDown();
            if(ClientPlayNetworking.canSend(VeinPackets.Held.TYPE)&&(down!=held[0]||down&&++heartbeat[0]>=10)){
                ClientPlayNetworking.send(new VeinPackets.Held(down));heartbeat[0]=0;
            }held[0]=down;
        });
        KeyMapping sell=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.balancedshop.sell",InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,key.getCategory()));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(sell.consumeClick()) if(client.screen==null) openSell();});
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("sellshop")
            .executes(context->{openSell();return 1;})));
        ClientPlayNetworking.registerGlobalReceiver(SellPackets.Page.TYPE,(page,context)->{
            if(context.client().screen instanceof SellScreen screen) screen.accept(page);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("bookshop")
            .executes(context->{openBooks();return 1;})));
        ClientPlayNetworking.registerGlobalReceiver(BookPackets.Page.TYPE,(page,context)->{
            if(context.client().screen instanceof BookScreen screen) screen.accept(page);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("shop")
            .executes(context->{open();return 1;})));
        ClientCommandRegistrationCallback.EVENT.register((d,a)->d.register(ClientCommandManager.literal("spawners").executes(ctx->{openSpawners();return 1;})));
        ClientPlayNetworking.registerGlobalReceiver(SpawnerPackets.Page.TYPE,(p,ctx)->{if(ctx.client().screen instanceof SpawnerScreen screen)screen.accept(p);});
        ClientPlayNetworking.registerGlobalReceiver(ShopPackets.Page.TYPE,(page,context)->{
            if(context.client().screen instanceof ShopScreen screen) screen.accept(page);
            if(context.client().screen instanceof MarketHubScreen screen)screen.accept(page);
        });
    }
    public static void open() {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null) return;
        if(!ClientPlayNetworking.canSend(ShopPackets.Query.TYPE)) {
            client.player.displayClientMessage(Component.translatable("shop.balancedshop.missing"),false);return;
        }
        client.setScreen(new MarketHubScreen());
    }
    public static void openCoins(net.minecraft.client.gui.screens.Screen parent){
        Minecraft c=Minecraft.getInstance();if(c.player==null)return;
        if(!ClientPlayNetworking.canSend(dev.eymen.balancedshop.CoinPackets.Query.TYPE)){c.player.displayClientMessage(Component.translatable("shop.balancedshop.missing"),false);return;}
        c.setScreen(new CoinScreen(parent));
    }
    public static void openShop(String category){Minecraft client=Minecraft.getInstance();if(client.player!=null&&ClientPlayNetworking.canSend(ShopPackets.Query.TYPE))client.setScreen(new ShopScreen(category));}
    public static void openSpawners(){Minecraft client=Minecraft.getInstance();if(client.player!=null&&ClientPlayNetworking.canSend(SpawnerPackets.Query.TYPE))client.setScreen(new SpawnerScreen());}
    public static void openSell(){
        Minecraft client=Minecraft.getInstance();if(client.player==null) return;
        if(!ClientPlayNetworking.canSend(SellPackets.Query.TYPE)){
            client.player.displayClientMessage(Component.translatable("shop.balancedshop.missing"),false);return;
        }
        client.setScreen(new SellScreen());
    }
    public static void openUpgrade(){
        Minecraft client=Minecraft.getInstance();if(client.player==null) return;
        if(!ClientPlayNetworking.canSend(dev.eymen.balancedshop.UpgradePackets.Query.TYPE)){client.player.displayClientMessage(Component.translatable("shop.balancedshop.missing"),false);return;}
        client.setScreen(new UpgradeScreen());
    }
    public static void openBooks(){
        Minecraft client=Minecraft.getInstance();if(client.player==null) return;
        if(!ClientPlayNetworking.canSend(BookPackets.Query.TYPE)){
            client.player.displayClientMessage(Component.translatable("shop.balancedshop.missing"),false);return;
        }
        client.setScreen(new BookScreen());
    }
}
