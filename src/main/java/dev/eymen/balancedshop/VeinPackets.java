package dev.eymen.balancedshop;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public final class VeinPackets {
    public record Held(boolean held) implements CustomPacketPayload {
        public static final Type<Held> TYPE=new Type<>(Identifier.fromNamespaceAndPath("balancedshop","vein_held"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Held> CODEC=new StreamCodec<>(){
            public Held decode(RegistryFriendlyByteBuf b){return new Held(b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,Held p){b.writeBoolean(p.held());}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
