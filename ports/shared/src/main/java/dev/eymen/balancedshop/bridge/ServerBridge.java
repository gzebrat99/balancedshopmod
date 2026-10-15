package dev.eymen.balancedshop.bridge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.EntityHitResult;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;

/** Small internal contract. Loader adapters attach these callbacks to real native APIs. */
public final class ServerBridge {
    public static Path configDir=Path.of("config");
    public interface ModInitializer {void onInitialize();}
    public static class Event<T> {
        public final List<T> listeners=new CopyOnWriteArrayList<>();
        public void register(T listener){listeners.add(listener);}
    }
    public static final class FabricLoader {
        private static final FabricLoader INSTANCE=new FabricLoader();
        public static FabricLoader getInstance(){return INSTANCE;}
        public Path getConfigDir(){return configDir;}
    }
    @FunctionalInterface public interface Reload {void accept(MinecraftServer server,Object resources,boolean success);}
    public static final class ServerLifecycleEvents {
        public static final Event<Consumer<MinecraftServer>> SERVER_STARTED=new Event<>(),SERVER_STOPPED=new Event<>();
        public static final Event<Reload> END_DATA_PACK_RELOAD=new Event<>();
    }
    public static final class ServerTickEvents {public static final Event<Consumer<MinecraftServer>> END_SERVER_TICK=new Event<>();}
    @FunctionalInterface public interface Join {void accept(ServerGamePacketListenerImpl handler,Object sender,MinecraftServer server);}
    @FunctionalInterface public interface Disconnect {void accept(ServerGamePacketListenerImpl handler,MinecraftServer server);}
    public static final class ServerPlayConnectionEvents {
        public static final Event<Join> JOIN=new Event<>();public static final Event<Disconnect> DISCONNECT=new Event<>();
    }
    @FunctionalInterface public interface CommandsRegistration {void accept(CommandDispatcher<CommandSourceStack> dispatcher,CommandBuildContext context,Commands.CommandSelection environment);}
    public static final class CommandRegistrationCallback {public static final Event<CommandsRegistration> EVENT=new Event<>();}
    @FunctionalInterface public interface UseEntity {InteractionResult accept(Player player,Level level,InteractionHand hand,Entity entity,EntityHitResult hit);}
    public static final class UseEntityCallback {
        public static final Event<UseEntity> EVENT=new Event<>();
        public static InteractionResult interact(Player player,InteractionHand hand,Entity entity,EntityHitResult hit){
            for(var callback:EVENT.listeners){var result=callback.accept(player,player.level(),hand,entity,hit);if(result!=InteractionResult.PASS)return result;}return InteractionResult.PASS;
        }
    }
    public interface Entries {void accept(ItemLike item);}
    public static final class ItemGroupEvents {
        public static final Map<ResourceKey<CreativeModeTab>,Event<Consumer<Entries>>> TABS=new LinkedHashMap<>();
        public static Event<Consumer<Entries>> modifyEntriesEvent(ResourceKey<CreativeModeTab> key){return TABS.computeIfAbsent(key,k->new Event<>());}
        public static void fill(ResourceKey<CreativeModeTab> key,Entries output){var event=TABS.get(key);if(event!=null)event.listeners.forEach(l->l.accept(output));}
    }
    public record Definition<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,boolean serverbound) {}
    public static final Map<Identifier,Definition<?>> DEFINITIONS=new LinkedHashMap<>();
    public static final class PayloadTypeRegistry {
        private final boolean serverbound;private PayloadTypeRegistry(boolean b){serverbound=b;}
        public static PayloadTypeRegistry playC2S(){return new PayloadTypeRegistry(true);}public static PayloadTypeRegistry playS2C(){return new PayloadTypeRegistry(false);}
        public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec){if(DEFINITIONS.putIfAbsent(type.id(),new Definition<>(type,codec,serverbound))!=null)throw new IllegalStateException("Duplicate payload "+type.id());}
    }
    public record Context(ServerPlayer player) {}
    public static final class ServerPlayNetworking {
        public static BiPredicate<ServerPlayer,CustomPacketPayload.Type<?>> available=(p,t)->false;
        public static BiConsumer<ServerPlayer,CustomPacketPayload> sender=(p,t)->{throw new IllegalStateException("Network adapter not installed");};
        private static final Map<Identifier,BiConsumer<CustomPacketPayload,Context>> handlers=new HashMap<>();
        public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Type<T> type,BiConsumer<T,Context> handler){handlers.put(type.id(),(p,c)->handler.accept((T)p,c));}
        public static boolean canSend(ServerPlayer player,CustomPacketPayload.Type<?> type){return available.test(player,type);}
        public static void send(ServerPlayer player,CustomPacketPayload payload){sender.accept(player,payload);}
        public static void receive(CustomPacketPayload payload,ServerPlayer player){var handler=handlers.get(payload.type().id());if(player==null||handler==null)throw new IllegalArgumentException("Unexpected server payload");handler.accept(payload,new Context(player));}
    }
}
