package dev.eymen.balancedshop.forge;
import dev.eymen.balancedshop.bridge.ClientBridge;
import dev.eymen.balancedshop.client.ShopClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
public final class ForgeClient {
    public static void install(BusGroup group){
        new ShopClient().onInitializeClient();ForgeNetwork.clientReceiver=ClientBridge.ClientPlayNetworking::receive;
        dev.eymen.balancedshop.bridge.VerificationHook.call("ClientVerification","install");
        ClientBridge.ClientPlayNetworking.available=t->{var c=Minecraft.getInstance().getConnection();return c!=null&&ForgeNetwork.CHANNEL.isRemotePresent(c.getConnection());};
        ClientBridge.ClientPlayNetworking.sender=p->ForgeNetwork.CHANNEL.send(new ForgeNetwork.ToServer(p),Minecraft.getInstance().getConnection().getConnection());
        RegisterKeyMappingsEvent.BUS.addListener(e->ClientBridge.KeyBindingHelper.KEYS.forEach(e::register));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e->ClientBridge.ClientTickEvents.END_CLIENT_TICK.listeners.forEach(l->l.accept(Minecraft.getInstance())));
        RegisterClientCommandsEvent.BUS.addListener(e->ClientBridge.ClientCommandRegistrationCallback.EVENT.listeners.forEach(l->l.accept(e.getDispatcher(),e.getBuildContext())));
        ItemTooltipEvent.BUS.addListener(e->ClientBridge.ItemTooltipCallback.EVENT.listeners.forEach(l->l.accept(e.getItemStack(),Item.TooltipContext.of(Minecraft.getInstance().level),e.getFlags(),e.getToolTip())));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(e->ClientBridge.ClientPlayConnectionEvents.DISCONNECT.listeners.forEach(l->l.accept(Minecraft.getInstance().getConnection(),Minecraft.getInstance())));
    }
}
