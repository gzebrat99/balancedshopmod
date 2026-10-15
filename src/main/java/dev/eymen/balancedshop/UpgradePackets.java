package dev.eymen.balancedshop;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class UpgradePackets {
    public static final int PAGE_SIZE=4;
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("balancedshop",path));}
    public record Query(int serial,int slot,boolean enchant,int page,String search,String target,int give,int receive) implements CustomPacketPayload {
        public Query(int serial,int slot,boolean enchant,int page,String search,String target){this(serial,slot,enchant,page,search,target,1,1);}
        public static final Type<Query> TYPE=UpgradePackets.type("upgrade_query_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=new StreamCodec<>(){
            public Query decode(RegistryFriendlyByteBuf b){return new Query(b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readVarInt(),b.readUtf(64),b.readUtf(300),b.readVarInt(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Query p){b.writeVarInt(p.serial);b.writeVarInt(p.slot);b.writeBoolean(p.enchant);b.writeVarInt(p.page);b.writeUtf(p.search,64);b.writeUtf(p.target,300);b.writeVarInt(p.give);b.writeVarInt(p.receive);}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Roll(int serial,long token) implements CustomPacketPayload {
        public static final Type<Roll> TYPE=UpgradePackets.type("upgrade_roll");
        public static final StreamCodec<RegistryFriendlyByteBuf,Roll> CODEC=new StreamCodec<>(){
            public Roll decode(RegistryFriendlyByteBuf b){return new Roll(b.readVarInt(),b.readLong());}
            public void encode(RegistryFriendlyByteBuf b,Roll p){b.writeVarInt(p.serial);b.writeLong(p.token);}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Page(int serial,long token,int slot,int page,int total,long balance,long value,double chance,long fee,boolean destroy,String selected,String message,List<ItemStack> inventory,List<UpgradeEngine.Target> targets,int give,int receive,int available) implements CustomPacketPayload {
        public static final Type<Page> TYPE=UpgradePackets.type("upgrade_page_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=new StreamCodec<>(){
            public Page decode(RegistryFriendlyByteBuf b){
                int serial=b.readVarInt();long token=b.readLong();int slot=b.readVarInt(),page=b.readVarInt(),total=b.readVarInt();long balance=b.readLong(),value=b.readLong();double chance=b.readDouble();long fee=b.readLong();boolean destroy=b.readBoolean();String selected=b.readUtf(300),message=b.readUtf(64);
                List<ItemStack> inventory=new ArrayList<>(36);for(int i=0;i<36;i++) inventory.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
                int size=b.readVarInt();if(size<0||size>PAGE_SIZE) throw new IllegalArgumentException("Invalid upgrade page");List<UpgradeEngine.Target> targets=new ArrayList<>(size);
                for(int i=0;i<size;i++) targets.add(new UpgradeEngine.Target(b.readUtf(300),ItemStack.STREAM_CODEC.decode(b),b.readLong()));
                return new Page(serial,token,slot,page,total,balance,value,chance,fee,destroy,selected,message,List.copyOf(inventory),List.copyOf(targets),b.readVarInt(),b.readVarInt(),b.readVarInt());
            }
            public void encode(RegistryFriendlyByteBuf b,Page p){
                b.writeVarInt(p.serial);b.writeLong(p.token);b.writeVarInt(p.slot);b.writeVarInt(p.page);b.writeVarInt(p.total);b.writeLong(p.balance);b.writeLong(p.value);b.writeDouble(p.chance);b.writeLong(p.fee);b.writeBoolean(p.destroy);b.writeUtf(p.selected,300);b.writeUtf(p.message,64);
                for(var stack:p.inventory) ItemStack.OPTIONAL_STREAM_CODEC.encode(b,stack);b.writeVarInt(p.targets.size());
                for(var target:p.targets){b.writeUtf(target.key(),300);ItemStack.STREAM_CODEC.encode(b,target.result());b.writeLong(target.value());}
                b.writeVarInt(p.give);b.writeVarInt(p.receive);b.writeVarInt(p.available);
            }
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
