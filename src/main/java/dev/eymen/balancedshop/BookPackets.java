package dev.eymen.balancedshop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class BookPackets {
    private BookPackets() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("balancedshop",path));}
    public record Query(int serial,int page,String search) implements CustomPacketPayload {
        public static final Type<Query> TYPE=BookPackets.type("books_query");
        public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=new StreamCodec<>() {
            public Query decode(RegistryFriendlyByteBuf b){return new Query(b.readVarInt(),b.readVarInt(),b.readUtf(64));}
            public void encode(RegistryFriendlyByteBuf b,Query p){b.writeVarInt(p.serial);b.writeVarInt(p.page);b.writeUtf(p.search,64);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Buy(int serial,long generation,String id,int level,int quantity) implements CustomPacketPayload {
        public static final Type<Buy> TYPE=BookPackets.type("books_buy");
        public static final StreamCodec<RegistryFriendlyByteBuf,Buy> CODEC=new StreamCodec<>() {
            public Buy decode(RegistryFriendlyByteBuf b){return new Buy(b.readVarInt(),b.readLong(),b.readUtf(256),b.readVarInt(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Buy p){b.writeVarInt(p.serial);b.writeLong(p.generation);b.writeUtf(p.id,256);b.writeVarInt(p.level);b.writeVarInt(p.quantity);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Page(int serial,long generation,int page,int total,long balance,String message,List<BookCatalogue.Entry> entries) implements CustomPacketPayload {
        public static final Type<Page> TYPE=BookPackets.type("books_page");
        public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=new StreamCodec<>() {
            public Page decode(RegistryFriendlyByteBuf b){
                int serial=b.readVarInt();long generation=b.readLong();int page=b.readVarInt(),total=b.readVarInt();long balance=b.readLong();String message=b.readUtf(64);
                int count=b.readVarInt();if(count<0||count>BookCatalogue.PAGE_SIZE) throw new IllegalArgumentException("Invalid book page");
                List<BookCatalogue.Entry> entries=new ArrayList<>(count);
                for(int i=0;i<count;i++) entries.add(new BookCatalogue.Entry(b.readUtf(256),b.readVarInt(),b.readLong(),ItemStack.STREAM_CODEC.decode(b)));
                return new Page(serial,generation,page,total,balance,message,List.copyOf(entries));
            }
            public void encode(RegistryFriendlyByteBuf b,Page p){
                b.writeVarInt(p.serial);b.writeLong(p.generation);b.writeVarInt(p.page);b.writeVarInt(p.total);b.writeLong(p.balance);b.writeUtf(p.message,64);b.writeVarInt(p.entries.size());
                for(var e:p.entries){b.writeUtf(e.id(),256);b.writeVarInt(e.level());b.writeLong(e.buy());ItemStack.STREAM_CODEC.encode(b,e.book());}
            }
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
