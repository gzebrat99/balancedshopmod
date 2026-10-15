package dev.eymen.balancedshop.legacy;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import java.util.*;
import java.util.function.BiConsumer;
public final class LegacyClientNetwork {
    public record ClientContext(Minecraft client) {}
    public static final class ClientPlayNetworking {
        public static <T extends LegacyPayload> void registerGlobalReceiver(LegacyPayload.Type<T> type,BiConsumer<T,ClientContext> handler){
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type.id(),(client,connection,buffer,sender)->{
                T packet=LegacyNetwork.<T>codec(type).decode(buffer);
                client.execute(()->handler.accept(packet,new ClientContext(client)));
            });
        }
        public static boolean canSend(LegacyPayload.Type<?> type){return net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(type.id());}
        public static void send(LegacyPayload payload){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload.type().id(),LegacyNetwork.encode(payload));}
    }
}
