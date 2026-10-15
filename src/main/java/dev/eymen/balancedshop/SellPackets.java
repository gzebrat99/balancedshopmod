package dev.eymen.balancedshop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class SellPackets {
    private SellPackets() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String id){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("balancedshop",id));}
    public record Query(int serial) implements CustomPacketPayload {
        public static final Type<Query> TYPE=SellPackets.type("sell_query");
        public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=new StreamCodec<>() {
            public Query decode(RegistryFriendlyByteBuf b){return new Query(b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Query p){b.writeVarInt(p.serial);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Action(int serial,String item,int quantity) implements CustomPacketPayload {
        public static final Type<Action> TYPE=SellPackets.type("sell_action");
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>() {
            public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readVarInt(),b.readUtf(256),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Action p){b.writeVarInt(p.serial);b.writeUtf(p.item,256);b.writeVarInt(p.quantity);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Slot(String id,int count,boolean plain,boolean sellable,long price,ItemStack stack) {
        public Slot(String id,int count,boolean plain,boolean sellable,long price){this(id,count,plain,sellable,price,ItemStack.EMPTY);}
    }
    public record Page(int serial,long balance,String message,List<Slot> inventory) implements CustomPacketPayload {
        public static final Type<Page> TYPE=SellPackets.type("sell_page_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=new StreamCodec<>() {
            public Page decode(RegistryFriendlyByteBuf b){
                int serial=b.readVarInt();long balance=b.readLong();String message=b.readUtf(64);
                List<Slot> slots=new ArrayList<>(36);
                for(int i=0;i<36;i++) slots.add(new Slot(b.readUtf(256),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readLong(),ItemStack.OPTIONAL_STREAM_CODEC.decode(b)));
                return new Page(serial,balance,message,List.copyOf(slots));
            }
            public void encode(RegistryFriendlyByteBuf b,Page p){
                b.writeVarInt(p.serial);b.writeLong(p.balance);b.writeUtf(p.message,64);
                for(Slot s:p.inventory){b.writeUtf(s.id,256);b.writeVarInt(s.count);b.writeBoolean(s.plain);b.writeBoolean(s.sellable);b.writeLong(s.price);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,s.stack);}
            }
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
