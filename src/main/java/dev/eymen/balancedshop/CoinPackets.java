package dev.eymen.balancedshop;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;
public final class CoinPackets {
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("balancedshop",path));}
    public record Query(int serial) implements CustomPacketPayload {
        public static final Type<Query> TYPE=CoinPackets.type("coin_query");
        public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=new StreamCodec<>(){public Query decode(RegistryFriendlyByteBuf b){return new Query(b.readVarInt());}public void encode(RegistryFriendlyByteBuf b,Query p){b.writeVarInt(p.serial);}};
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Action(int serial,long token,int operation,int denomination,int quantity) implements CustomPacketPayload {
        public static final Type<Action> TYPE=CoinPackets.type("coin_action");
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>(){public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readVarInt(),b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt());}public void encode(RegistryFriendlyByteBuf b,Action p){b.writeVarInt(p.serial);b.writeLong(p.token);b.writeVarInt(p.operation);b.writeVarInt(p.denomination);b.writeVarInt(p.quantity);}};
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Page(int serial,long token,long balance,long physical,List<Integer> counts,String message) implements CustomPacketPayload {
        public static final Type<Page> TYPE=CoinPackets.type("coin_page");
        public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=new StreamCodec<>(){public Page decode(RegistryFriendlyByteBuf b){int serial=b.readVarInt();long token=b.readLong(),balance=b.readLong(),physical=b.readLong();List<Integer> counts=new ArrayList<>(4);for(int i=0;i<4;i++) counts.add(b.readVarInt());return new Page(serial,token,balance,physical,List.copyOf(counts),b.readUtf(64));}public void encode(RegistryFriendlyByteBuf b,Page p){b.writeVarInt(p.serial);b.writeLong(p.token);b.writeLong(p.balance);b.writeLong(p.physical);for(int n:p.counts) b.writeVarInt(n);b.writeUtf(p.message,64);}};
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
