package dev.eymen.balancedshop.bridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.*;
import dev.eymen.balancedshop.bridge.ServerBridge.Event;

/** Client-only contract; this class is never loaded on a dedicated server. */
public final class ClientBridge {
    public interface ClientModInitializer {void onInitializeClient();}
    public static final class ClientTickEvents {public static final Event<Consumer<Minecraft>> END_CLIENT_TICK=new Event<>();}
    public static final class KeyBindingHelper {
        public static final List<KeyMapping> KEYS=new ArrayList<>();
        public static KeyMapping registerKeyBinding(KeyMapping key){KEYS.add(key);return key;}
    }
    @FunctionalInterface public interface CommandRegistration {void accept(CommandDispatcher<CommandSourceStack> dispatcher,CommandBuildContext context);}
    public static final class ClientCommandRegistrationCallback {public static final Event<CommandRegistration> EVENT=new Event<>();}
    public static final class ClientCommandManager {public static LiteralArgumentBuilder<CommandSourceStack> literal(String literal){return Commands.literal(literal);}}
    @FunctionalInterface public interface Tooltip {void accept(ItemStack stack,Item.TooltipContext context,TooltipFlag flag,List<Component> lines);}
    public static final class ItemTooltipCallback {public static final Event<Tooltip> EVENT=new Event<>();}
    @FunctionalInterface public interface Disconnect {void accept(ClientPacketListener handler,Minecraft client);}
    public static final class ClientPlayConnectionEvents {public static final Event<Disconnect> DISCONNECT=new Event<>();}
    public record Context(Minecraft client) {}
    public static final class ClientPlayNetworking {
        public static Predicate<CustomPacketPayload.Type<?>> available=t->false;
        public static Consumer<CustomPacketPayload> sender=t->{throw new IllegalStateException("Network adapter not installed");};
        private static final Map<Identifier,BiConsumer<CustomPacketPayload,Context>> handlers=new HashMap<>();
        public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Type<T> type,BiConsumer<T,Context> handler){handlers.put(type.id(),(p,c)->handler.accept((T)p,c));}
        public static boolean canSend(CustomPacketPayload.Type<?> type){return available.test(type);}
        public static void send(CustomPacketPayload payload){sender.accept(payload);}
        public static void receive(CustomPacketPayload payload){var handler=handlers.get(payload.type().id());if(handler==null)throw new IllegalArgumentException("Unexpected client payload");handler.accept(payload,new Context(Minecraft.getInstance()));}
    }
}
