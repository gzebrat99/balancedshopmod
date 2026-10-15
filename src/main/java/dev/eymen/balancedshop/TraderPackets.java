package dev.eymen.balancedshop;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;
public final class TraderPackets {
    public static final int PAGE_SIZE=8;
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("balancedshop",path));}
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE=TraderPackets.type("trader_open");public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.unit(new Open());public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Query(int serial,int page,String search) implements CustomPacketPayload {
        public static final Type<Query> TYPE=TraderPackets.type("trader_query");public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=new StreamCodec<>(){public Query decode(RegistryFriendlyByteBuf b){return new Query(b.readVarInt(),b.readVarInt(),b.readUtf(64));}public void encode(RegistryFriendlyByteBuf b,Query p){b.writeVarInt(p.serial);b.writeVarInt(p.page);b.writeUtf(p.search,64);}};public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Buy(int serial,long token,String id,int quantity) implements CustomPacketPayload {
        public static final Type<Buy> TYPE=TraderPackets.type("trader_buy");public static final StreamCodec<RegistryFriendlyByteBuf,Buy> CODEC=new StreamCodec<>(){public Buy decode(RegistryFriendlyByteBuf b){return new Buy(b.readVarInt(),b.readLong(),b.readUtf(256),b.readVarInt());}public void encode(RegistryFriendlyByteBuf b,Buy p){b.writeVarInt(p.serial);b.writeLong(p.token);b.writeUtf(p.id,256);b.writeVarInt(p.quantity);}};public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Page(int serial,long token,int page,int total,long physical,String message,List<ShopPackets.Entry> entries) implements CustomPacketPayload {
        public static final Type<Page> TYPE=TraderPackets.type("trader_page");public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=new StreamCodec<>(){
            public Page decode(RegistryFriendlyByteBuf b){int serial=b.readVarInt();long token=b.readLong();int page=b.readVarInt(),total=b.readVarInt();long physical=b.readLong();String message=b.readUtf(64);int n=b.readVarInt();if(n<0||n>PAGE_SIZE) throw new IllegalArgumentException("Invalid Create page");List<ShopPackets.Entry> entries=new ArrayList<>(n);for(int i=0;i<n;i++) entries.add(new ShopPackets.Entry(b.readUtf(256),b.readLong(),0,true,false));return new Page(serial,token,page,total,physical,message,List.copyOf(entries));}
            public void encode(RegistryFriendlyByteBuf b,Page p){b.writeVarInt(p.serial);b.writeLong(p.token);b.writeVarInt(p.page);b.writeVarInt(p.total);b.writeLong(p.physical);b.writeUtf(p.message,64);b.writeVarInt(p.entries.size());for(var entry:p.entries){b.writeUtf(entry.id(),256);b.writeLong(entry.buy());}}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
