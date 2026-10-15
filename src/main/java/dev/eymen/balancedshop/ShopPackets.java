package dev.eymen.balancedshop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public final class ShopPackets {
    private ShopPackets() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("balancedshop", path));
    }
    public record Query(int serial, int page, String search, String namespace) implements CustomPacketPayload {
        public static final Type<Query> TYPE = ShopPackets.type("query");
        public static final StreamCodec<RegistryFriendlyByteBuf, Query> CODEC = new StreamCodec<>() {
            public Query decode(RegistryFriendlyByteBuf b) { return new Query(b.readVarInt(), b.readVarInt(), b.readUtf(64), b.readUtf(64)); }
            public void encode(RegistryFriendlyByteBuf b, Query p) { b.writeVarInt(p.serial); b.writeVarInt(p.page); b.writeUtf(p.search,64); b.writeUtf(p.namespace,64); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Trade(String item, int quantity, boolean sell) implements CustomPacketPayload {
        public static final Type<Trade> TYPE = ShopPackets.type("trade");
        public static final StreamCodec<RegistryFriendlyByteBuf, Trade> CODEC = new StreamCodec<>() {
            public Trade decode(RegistryFriendlyByteBuf b) { return new Trade(b.readUtf(256), b.readVarInt(), b.readBoolean()); }
            public void encode(RegistryFriendlyByteBuf b, Trade p) { b.writeUtf(p.item,256); b.writeVarInt(p.quantity); b.writeBoolean(p.sell); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Entry(String id, long buy, long sell, boolean buyable, boolean sellable) {}
    public record Page(int serial, int page, int total, long balance, String message,
                       List<String> namespaces, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Page> TYPE = ShopPackets.type("page");
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = new StreamCodec<>() {
            public Page decode(RegistryFriendlyByteBuf b) {
                int serial=b.readVarInt(), page=b.readVarInt(), total=b.readVarInt();
                long balance=b.readLong(); String message=b.readUtf(64);
                int count=b.readVarInt(); if(count<0 || count>256) throw new IllegalArgumentException("Invalid namespaces");
                List<String> namespaces=new ArrayList<>(count);
                for(int i=0;i<count;i++) namespaces.add(b.readUtf(64));
                count=b.readVarInt(); if(count<0 || count>45) throw new IllegalArgumentException("Invalid shop page");
                List<Entry> entries=new ArrayList<>(count);
                for(int i=0;i<count;i++) entries.add(new Entry(b.readUtf(256),b.readLong(),b.readLong(),b.readBoolean(),b.readBoolean()));
                return new Page(serial,page,total,balance,message,List.copyOf(namespaces),List.copyOf(entries));
            }
            public void encode(RegistryFriendlyByteBuf b, Page p) {
                b.writeVarInt(p.serial); b.writeVarInt(p.page); b.writeVarInt(p.total); b.writeLong(p.balance); b.writeUtf(p.message,64);
                b.writeVarInt(p.namespaces.size()); for(String s:p.namespaces) b.writeUtf(s,64);
                b.writeVarInt(p.entries.size()); for(Entry e:p.entries) {
                    b.writeUtf(e.id,256); b.writeLong(e.buy); b.writeLong(e.sell); b.writeBoolean(e.buyable); b.writeBoolean(e.sellable);
                }
            }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
