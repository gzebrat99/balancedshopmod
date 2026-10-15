package dev.eymen.balancedshop.neoforge;
import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.bridge.ServerBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("balancedshop")
public final class NeoEntrypoint {
    public NeoEntrypoint(IEventBus modBus){
        ServerBridge.configDir=FMLPaths.CONFIGDIR.get();
        new BalancedShop().onInitialize();
        modBus.addListener((RegisterEvent e)->{if(e.getRegistryKey().equals(Registries.BLOCK))ModContent.registerBlocks();else if(e.getRegistryKey().equals(Registries.ITEM))ModContent.registerItems();});
        modBus.addListener((BuildCreativeModeTabContentsEvent e)->ServerBridge.ItemGroupEvents.fill(e.getTabKey(),e::accept));
        modBus.addListener((RegisterPayloadHandlersEvent e)->ServerBridge.DEFINITIONS.values().forEach(d->register(e,d)));
        ServerBridge.ServerPlayNetworking.available=(p,t)->NetworkRegistry.hasChannel(p.connection,t.id());
        ServerBridge.ServerPlayNetworking.sender=PacketDistributor::sendToPlayer;
        var bus=NeoForge.EVENT_BUS;
        bus.addListener((ServerStartedEvent e)->ServerBridge.ServerLifecycleEvents.SERVER_STARTED.listeners.forEach(l->l.accept(e.getServer())));
        bus.addListener((ServerStoppedEvent e)->ServerBridge.ServerLifecycleEvents.SERVER_STOPPED.listeners.forEach(l->l.accept(e.getServer())));
        bus.addListener((ServerTickEvent.Post e)->ServerBridge.ServerTickEvents.END_SERVER_TICK.listeners.forEach(l->l.accept(e.getServer())));
        bus.addListener((RegisterCommandsEvent e)->ServerBridge.CommandRegistrationCallback.EVENT.listeners.forEach(l->l.accept(e.getDispatcher(),e.getBuildContext(),e.getCommandSelection())));
        bus.addListener((OnDatapackSyncEvent e)->{if(e.getPlayer()==null)ServerBridge.ServerLifecycleEvents.END_DATA_PACK_RELOAD.listeners.forEach(l->l.accept(e.getPlayerList().getServer(),null,true));});
        bus.addListener((PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer p)ServerBridge.ServerPlayConnectionEvents.JOIN.listeners.forEach(l->l.accept(p.connection,null,p.level().getServer()));});
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e)->{if(e.getEntity() instanceof ServerPlayer p)ServerBridge.ServerPlayConnectionEvents.DISCONNECT.listeners.forEach(l->l.accept(p.connection,p.level().getServer()));});
        bus.addListener((PlayerInteractEvent.EntityInteract e)->{var result=ServerBridge.UseEntityCallback.interact(e.getEntity(),e.getHand(),e.getTarget(),null);if(result!=InteractionResult.PASS){e.setCancellationResult(result);e.setCanceled(true);}});
        if(FMLEnvironment.getDist().isClient()) NeoClient.install(modBus);
        dev.eymen.balancedshop.bridge.VerificationHook.call("ServerVerification","install");
    }
    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void register(RegisterPayloadHandlersEvent event,ServerBridge.Definition<T> d){
        var registrar=event.registrar("1");
        if(d.serverbound())registrar.playToServer(d.type(),d.codec(),(p,c)->ServerBridge.ServerPlayNetworking.receive(p,(ServerPlayer)c.player()));
        else registrar.playToClient(d.type(),d.codec());
    }
}
