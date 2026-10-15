package dev.eymen.balancedshop.verification;
import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.bridge.ClientBridge;
import dev.eymen.balancedshop.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;
public final class ClientVerification {
    private static int ticks,phase,checks,screenTicks;
    private static boolean launched;
    public static void install(){ClientBridge.ClientTickEvents.END_CLIENT_TICK.register(ClientVerification::tick);}
    private static void check(boolean result,String label){if(!result)throw new AssertionError(label);checks++;}
    private static void tick(Minecraft client){
        try{
            if(++ticks>6000)throw new AssertionError("Client verification timed out, phase="+phase);
            if(!launched){
                if(client.getOverlay()!=null||ticks<100)return;
                client.options.pauseOnLostFocus=false;
                org.lwjgl.glfw.GLFW.glfwHideWindow(client.getWindow().getWindow());
                BalancedShop.LOG.info("Opening verification world from screen {}",client.screen==null?"null":client.screen.getClass().getName());
                launched=true;client.createWorldOpenFlows().openWorld("verification",()->{});return;
            }
            if(client.player==null||!ServerVerification.ready)return;
            if(phase==0){
                if(++screenTicks<100)return;
                check(client.getResourceManager().getResource(ResourceLocation.parse("balancedshop:textures/item/coin.png")).isPresent(),"Coin texture loaded");
                check(!Component.translatable("shop.balancedshop.title").getString().equals("shop.balancedshop.title"),"Translation loaded");
                var tooltip=new ArrayList<Component>();SellTooltip.append(new ItemStack(Items.DIAMOND),tooltip);check(!tooltip.isEmpty(),"Inventory sale tooltip synced");
                phase=1;screenTicks=0;
            }
            if(phase<=8){
                if(screenTicks++==0){
                    Screen screen=switch(phase){case 1->new ShopScreen();case 2->new SellScreen();case 3->new BookScreen();case 4->new SpawnerScreen();case 5->new UpgradeScreen();case 6->new CoinScreen(null);case 7->new CreateTraderScreen();default->new MarketHubScreen();};
                    client.setScreen(screen);return;
                }
                if(screenTicks<15)return;
                Screen screen=client.screen;if(screen==null)throw new AssertionError("GUI closed unexpectedly "+phase);
                java.lang.reflect.Field field;
                try{field=screen.getClass().getDeclaredField("data");}
                catch(NoSuchFieldException e){throw new AssertionError("Unexpected screen "+screen.getClass().getName()+" in phase "+phase,e);}
                field.setAccessible(true);
                if(field.get(screen)==null&&screenTicks<160)return;
                check(screen!=null,"GUI remains open "+phase);
                check(field.get(screen)!=null,"Native server response for GUI "+phase);check(screen.children().size()>1,"GUI widgets initialized "+phase);
                check(screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.AbstractWidget).map(child->(net.minecraft.client.gui.components.AbstractWidget)child).allMatch(widget->widget.getX()>=0&&widget.getY()>=0&&widget.getX()+widget.getWidth()<=screen.width&&widget.getY()+widget.getHeight()<=screen.height),"GUI controls fit window "+phase);
                net.minecraft.client.Screenshot.grab(new java.io.File("."),"market-screen-"+phase+".png",client.getMainRenderTarget(),notice->{});
                phase++;screenTicks=0;return;
            }
            Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/client.txt"),"PASS "+checks+" assertions\nAll 8 GUIs initialized/rendered and received native packets\n");BalancedShop.LOG.info("Client backport verification PASS: {} assertions",checks);client.stop();
        }catch(Throwable e){try{Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/CLIENT-FAILED.txt"),e.toString());}catch(Exception ignored){}BalancedShop.LOG.error("Client backport verification failed",e);client.stop();}
    }
}
