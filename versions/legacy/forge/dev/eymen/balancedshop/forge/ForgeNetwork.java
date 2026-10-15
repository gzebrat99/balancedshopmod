package dev.eymen.balancedshop.forge;
import dev.eymen.balancedshop.bridge.ServerBridge;
import dev.eymen.balancedshop.legacy.LegacyPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Consumer;
public final class ForgeNetwork {
    public static SimpleChannel CHANNEL;
    public static Consumer<LegacyPayload> clientReceiver=p->{throw new IllegalStateException("Client handler on server");};
    public record ToServer(LegacyPayload payload) {}
    public record ToClient(LegacyPayload payload) {}
    private static LegacyPayload decode(FriendlyByteBuf buffer,boolean serverbound){
        var id=buffer.readResourceLocation();var d=ServerBridge.DEFINITIONS.get(id);
        if(d==null||d.serverbound()!=serverbound)throw new IllegalArgumentException("Invalid payload flow/type");
        return d.codec().decode(buffer);
    }
    @SuppressWarnings("unchecked") private static void encode(FriendlyByteBuf buffer,LegacyPayload payload,boolean serverbound){
        var d=(ServerBridge.Definition<LegacyPayload>)ServerBridge.DEFINITIONS.get(payload.type().id());
        if(d==null||d.serverbound()!=serverbound)throw new IllegalArgumentException("Invalid payload flow/type");
        buffer.writeResourceLocation(payload.type().id());d.codec().encode(buffer,payload);
    }
    public static void initialize(){
        CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("balancedshop","main"),()->"1","1"::equals,"1"::equals);
        CHANNEL.registerMessage(0,ToServer.class,(p,b)->encode(b,p.payload(),true),b->new ToServer(decode(b,true)),(p,s)->{var c=s.get();c.enqueueWork(()->ServerBridge.ServerPlayNetworking.receive(p.payload(),c.getSender()));c.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,ToClient.class,(p,b)->encode(b,p.payload(),false),b->new ToClient(decode(b,false)),(p,s)->{var c=s.get();c.enqueueWork(()->clientReceiver.accept(p.payload()));c.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        ServerBridge.ServerPlayNetworking.available=(p,t)->CHANNEL.isRemotePresent(p.connection.connection);
        ServerBridge.ServerPlayNetworking.sender=(p,packet)->CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new ToClient(packet));
    }
}
