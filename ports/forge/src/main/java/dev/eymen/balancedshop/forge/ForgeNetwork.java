package dev.eymen.balancedshop.forge;
import dev.eymen.balancedshop.bridge.ServerBridge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.*;
import java.util.function.Consumer;

/** Native Forge channel preserves registry-aware item/enchantment codecs and validates flow. */
public final class ForgeNetwork {
    public static SimpleChannel CHANNEL;
    public static Consumer<CustomPacketPayload> clientReceiver=p->{throw new IllegalStateException("Client handler on server");};
    public record ToServer(CustomPacketPayload payload) {}
    public record ToClient(CustomPacketPayload payload) {}
    private static CustomPacketPayload decode(RegistryFriendlyByteBuf buffer,boolean serverbound){
        var id=buffer.readIdentifier();var definition=ServerBridge.DEFINITIONS.get(id);
        if(definition==null||definition.serverbound()!=serverbound)throw new IllegalArgumentException("Invalid packet flow/type: "+id);
        return definition.codec().decode(buffer);
    }
    @SuppressWarnings("unchecked") private static void encode(RegistryFriendlyByteBuf b,CustomPacketPayload p,boolean serverbound){
        var definition=(ServerBridge.Definition<CustomPacketPayload>)ServerBridge.DEFINITIONS.get(p.type().id());
        if(definition==null||definition.serverbound()!=serverbound)throw new IllegalArgumentException("Invalid packet flow/type");
        b.writeIdentifier(p.type().id());definition.codec().encode(b,p);
    }
    public static void initialize(){
        CHANNEL=ChannelBuilder.named(Identifier.parse("balancedshop:main")).networkProtocolVersion(1).simpleChannel();
        var protocol=CHANNEL.protocol(NetworkProtocol.PLAY);
        protocol.flow(PacketFlow.SERVERBOUND).add(ToServer.class,new StreamCodec<RegistryFriendlyByteBuf,ToServer>(){
            public ToServer decode(RegistryFriendlyByteBuf b){return new ToServer(ForgeNetwork.decode(b,true));}
            public void encode(RegistryFriendlyByteBuf b,ToServer p){ForgeNetwork.encode(b,p.payload(),true);}
        },(p,c)->{c.enqueueWork(()->ServerBridge.ServerPlayNetworking.receive(p.payload(),c.getSender()));c.setPacketHandled(true);});
        protocol.flow(PacketFlow.CLIENTBOUND).add(ToClient.class,new StreamCodec<RegistryFriendlyByteBuf,ToClient>(){
            public ToClient decode(RegistryFriendlyByteBuf b){return new ToClient(ForgeNetwork.decode(b,false));}
            public void encode(RegistryFriendlyByteBuf b,ToClient p){ForgeNetwork.encode(b,p.payload(),false);}
        },(p,c)->{c.enqueueWork(()->clientReceiver.accept(p.payload()));c.setPacketHandled(true);});
        CHANNEL.build();
        ServerBridge.ServerPlayNetworking.available=(p,t)->CHANNEL.isRemotePresent(p.connection.getConnection());
        ServerBridge.ServerPlayNetworking.sender=(p,packet)->CHANNEL.send(new ToClient(packet),p.connection.getConnection());
    }
}
