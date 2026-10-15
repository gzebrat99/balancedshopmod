package dev.eymen.balancedshop;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class PriceSync {
    public record Price(String id,long buy,long sell,boolean sellable) {}
    public record Packet(long generation,boolean reset,List<Price> prices) implements CustomPacketPayload {
        public static final Type<Packet> TYPE=new Type<>(Identifier.fromNamespaceAndPath("balancedshop","tooltip_prices_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Packet> CODEC=new StreamCodec<>(){
            public Packet decode(RegistryFriendlyByteBuf b){long generation=b.readLong();boolean reset=b.readBoolean();int n=b.readVarInt();if(n<0||n>256) throw new IllegalArgumentException("Invalid price chunk");List<Price> prices=new ArrayList<>(n);for(int i=0;i<n;i++) prices.add(new Price(b.readUtf(256),b.readLong(),b.readLong(),b.readBoolean()));return new Packet(generation,reset,List.copyOf(prices));}
            public void encode(RegistryFriendlyByteBuf b,Packet p){b.writeLong(p.generation);b.writeBoolean(p.reset);b.writeVarInt(p.prices.size());for(var entry:p.prices){b.writeUtf(entry.id,256);b.writeLong(entry.buy);b.writeLong(entry.sell);b.writeBoolean(entry.sellable);}}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static List<Price> prices=List.of();private static long generation;
    public static void register(){PayloadTypeRegistry.playS2C().register(Packet.TYPE,Packet.CODEC);ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->send(handler.getPlayer()));}
    public static void configure(Catalogue catalogue,MinecraftServer server){List<Price> all=new ArrayList<>();for(var e:catalogue.all()) all.add(new Price(e.id(),e.buy(),e.sell(),e.sellable()));for(var e:catalogue.spawners())all.add(new Price("spawner@"+e.id(),e.buy(),e.sell(),catalogue.entry("balancedshop:mob_spawner").sellable()));catalogue.enchantmentPrices().forEach((id,price)->all.add(new Price(id,price,0,false)));prices=List.copyOf(all);generation++;for(var player:server.getPlayerList().getPlayers()) send(player);}
    public static void clear(){prices=List.of();generation++;}
    private static void send(ServerPlayer player){if(!ServerPlayNetworking.canSend(player,Packet.TYPE)) return;for(int i=0;i<prices.size();i+=256) ServerPlayNetworking.send(player,new Packet(generation,i==0,List.copyOf(prices.subList(i,Math.min(i+256,prices.size())))));}
}
