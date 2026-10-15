package dev.eymen.balancedshop.forge;
import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.bridge.ServerBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.registries.RegisterEvent;
@Mod("balancedshop")
public final class ForgeEntrypoint {
    public ForgeEntrypoint(FMLJavaModLoadingContext context){
        ServerBridge.configDir=FMLPaths.CONFIGDIR.get();new BalancedShop().onInitialize();ForgeNetwork.initialize();
        var group=context.getModBusGroup();
        RegisterEvent.getBus(group).addListener(e->{if(e.getRegistryKey().equals(Registries.BLOCK))ModContent.registerBlocks();else if(e.getRegistryKey().equals(Registries.ITEM))ModContent.registerItems();});
        BuildCreativeModeTabContentsEvent.BUS.addListener(e->ServerBridge.ItemGroupEvents.fill(e.getTabKey(),e::accept));
        ServerStartedEvent.BUS.addListener(e->ServerBridge.ServerLifecycleEvents.SERVER_STARTED.listeners.forEach(l->l.accept(e.getServer())));
        ServerStoppedEvent.BUS.addListener(e->ServerBridge.ServerLifecycleEvents.SERVER_STOPPED.listeners.forEach(l->l.accept(e.getServer())));
        TickEvent.ServerTickEvent.Post.BUS.addListener(e->ServerBridge.ServerTickEvents.END_SERVER_TICK.listeners.forEach(l->l.accept(e.server())));
        RegisterCommandsEvent.BUS.addListener(e->ServerBridge.CommandRegistrationCallback.EVENT.listeners.forEach(l->l.accept(e.getDispatcher(),e.getBuildContext(),e.getCommandSelection())));
        OnDatapackSyncEvent.BUS.addListener(e->{if(e.getPlayer()==null)ServerBridge.ServerLifecycleEvents.END_DATA_PACK_RELOAD.listeners.forEach(l->l.accept(e.getPlayerList().getServer(),null,true));});
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e->{if(e.getEntity() instanceof ServerPlayer p)ServerBridge.ServerPlayConnectionEvents.JOIN.listeners.forEach(l->l.accept(p.connection,null,p.level().getServer()));});
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e->{if(e.getEntity() instanceof ServerPlayer p)ServerBridge.ServerPlayConnectionEvents.DISCONNECT.listeners.forEach(l->l.accept(p.connection,p.level().getServer()));});
        PlayerInteractEvent.EntityInteract.BUS.addListener(e->{var result=ServerBridge.UseEntityCallback.interact(e.getEntity(),e.getHand(),e.getTarget(),null);if(result!=InteractionResult.PASS){e.setCancellationResult(result);return true;}return false;});
        if(FMLEnvironment.dist.isClient())ForgeClient.install(group);
        dev.eymen.balancedshop.bridge.VerificationHook.call("ServerVerification","install");
        BalancedShop.LOG.info("Initialized Forge Balanced Shop on {} (verification={})",FMLEnvironment.dist,Boolean.getBoolean("balancedshop.verify"));
    }
}
