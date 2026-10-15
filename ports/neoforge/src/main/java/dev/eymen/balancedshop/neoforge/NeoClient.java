package dev.eymen.balancedshop.neoforge;
import dev.eymen.balancedshop.bridge.*;
import dev.eymen.balancedshop.client.ShopClient;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
public final class NeoClient {
    public static void install(IEventBus modBus){
        new ShopClient().onInitializeClient();
        VerificationHook.call("ClientVerification","install");
        ClientBridge.ClientPlayNetworking.available=t->{var c=Minecraft.getInstance().getConnection();return c!=null&&NetworkRegistry.hasChannel(c,t.id());};
        ClientBridge.ClientPlayNetworking.sender=ClientPacketDistributor::sendToServer;
        modBus.addListener((RegisterKeyMappingsEvent e)->ClientBridge.KeyBindingHelper.KEYS.forEach(e::register));
        modBus.addListener((RegisterClientPayloadHandlersEvent e)->ServerBridge.DEFINITIONS.values().stream().filter(d->!d.serverbound()).forEach(d->register(e,d)));
        var bus=NeoForge.EVENT_BUS;
        bus.addListener((ClientTickEvent.Post e)->ClientBridge.ClientTickEvents.END_CLIENT_TICK.listeners.forEach(l->l.accept(Minecraft.getInstance())));
        bus.addListener((RegisterClientCommandsEvent e)->ClientBridge.ClientCommandRegistrationCallback.EVENT.listeners.forEach(l->l.accept(e.getDispatcher(),e.getBuildContext())));
        bus.addListener((ItemTooltipEvent e)->ClientBridge.ItemTooltipCallback.EVENT.listeners.forEach(l->l.accept(e.getItemStack(),e.getContext(),e.getFlags(),e.getToolTip())));
        bus.addListener((ClientPlayerNetworkEvent.LoggingOut e)->ClientBridge.ClientPlayConnectionEvents.DISCONNECT.listeners.forEach(l->l.accept(Minecraft.getInstance().getConnection(),Minecraft.getInstance())));
    }
    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void register(RegisterClientPayloadHandlersEvent e,ServerBridge.Definition<T> d){e.register(d.type(),(p,c)->ClientBridge.ClientPlayNetworking.receive(p));}
}
