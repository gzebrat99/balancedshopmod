package dev.eymen.balancedshop.platform;

import dev.eymen.balancedshop.network.*;
import dev.eymen.balancedshop.keybind.MarketKeybind;
import dev.eymen.balancedshop.placement.PlacementClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Physical client only: common/dedicated-server code never loads Minecraft client classes. */
public final class FabricClient {
    public static void install(){
        FabricTransport.clientSender=packet->ClientPlayNetworking.send(FabricTransport.C2S,FabricTransport.encodeClient(packet));
        ClientPlayNetworking.registerGlobalReceiver(FabricTransport.S2C,(client,handler,buffer,response)->{
            var page=MarketNetwork.Page.decode(buffer);client.execute(()->MarketNetwork.receiver.accept(page));
        });
        MarketKeybind.install();PlacementClient.install();
    }
}
