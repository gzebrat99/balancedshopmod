package dev.eymen.balancedshop.legacy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import java.util.*;
import java.util.function.BiConsumer;
public final class LegacyNetwork {
    private static final Map<LegacyPayload.Type<?>,LegacyCodec<FriendlyByteBuf,?>> codecs=new HashMap<>();
    public static final class PayloadTypeRegistry {
        public static PayloadTypeRegistry playC2S(){return new PayloadTypeRegistry();}
        public static PayloadTypeRegistry playS2C(){return new PayloadTypeRegistry();}
        public <T extends LegacyPayload> void register(LegacyPayload.Type<T> type,LegacyCodec<FriendlyByteBuf,T> codec){codecs.put(type,codec);}
    }
    public record ServerContext(ServerPlayer player) {}
    @SuppressWarnings("unchecked") public static <T extends LegacyPayload> LegacyCodec<FriendlyByteBuf,T> codec(LegacyPayload.Type<?> type){return (LegacyCodec<FriendlyByteBuf,T>)Objects.requireNonNull(codecs.get(type));}
    public static FriendlyByteBuf encode(LegacyPayload payload){var buffer=PacketByteBufs.create();codec(payload.type()).encode(buffer,payload);return buffer;}
    public static final class ServerPlayNetworking {
        public static <T extends LegacyPayload> void registerGlobalReceiver(LegacyPayload.Type<T> type,BiConsumer<T,ServerContext> handler){
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(type.id(),(server,player,connection,buffer,sender)->{
                T packet=LegacyNetwork.<T>codec(type).decode(buffer);
                server.execute(()->handler.accept(packet,new ServerContext(player)));
            });
        }
        public static boolean canSend(ServerPlayer player,LegacyPayload.Type<?> type){return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,type.id());}
        public static void send(ServerPlayer player,LegacyPayload payload){net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,payload.type().id(),encode(payload));}
    }
}
