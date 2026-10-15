package dev.eymen.balancedshop.network;

import dev.eymen.balancedshop.placement.AreaPlacement;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;

/** Decode on the network thread, execute mutations only on the server thread. */
public final class FabricTransport {
    public static final ResourceLocation C2S=new ResourceLocation("balancedshop","modern_c2s"),S2C=new ResourceLocation("balancedshop","modern_s2c");
    public static Consumer<Object> clientSender=packet->{};
    public static void install(){
        ServerPlayNetworking.registerGlobalReceiver(C2S,(server,player,handler,buffer,response)->{
            int operation=buffer.readUnsignedByte();
            if(operation==0)server.execute(()->MarketNetwork.open(player));
            else if(operation==1){var request=MarketNetwork.Request.decode(buffer);server.execute(()->MarketNetwork.handle(player,request));}
            else if(operation==2){int size=buffer.readVarInt();server.execute(()->AreaPlacement.handle(player,size));}
        });
    }
    public void sendToServer(Object packet){clientSender.accept(packet);}
    public void send(ServerPlayer player,MarketNetwork.Page page){var buffer=PacketByteBufs.create();page.encode(buffer);ServerPlayNetworking.send(player,S2C,buffer);}
    public static FriendlyByteBuf encodeClient(Object packet){
        var buffer=PacketByteBufs.create();
        if(packet instanceof MarketNetwork.Open)buffer.writeByte(0);
        else if(packet instanceof MarketNetwork.Request request){buffer.writeByte(1);request.encode(buffer);}
        else if(packet instanceof AreaPlacement.Request request){buffer.writeByte(2);buffer.writeVarInt(request.size());}
        else throw new IllegalArgumentException("Unknown Fabric packet");
        return buffer;
    }
}
